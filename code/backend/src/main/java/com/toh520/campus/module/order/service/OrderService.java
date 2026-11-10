package com.toh520.campus.module.order.service;

import com.toh520.campus.common.*;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.nutrition.service.NutritionService;
import com.toh520.campus.module.order.repository.OrderRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 食堂共用手动餐与 AI 套餐购物车；只有本人确认后才保存菜品快照、份数和订单。 */
@Service
public class OrderService {
  private final OrderRepository orders;
  private final NutritionService nutrition;
  private final TransactionTemplate transactions;

  public OrderService(
      OrderRepository orders, NutritionService nutrition, TransactionTemplate transactions) {
    this.orders = orders;
    this.nutrition = nutrition;
    this.transactions = transactions;
  }

  public Object cart(LoginSession session) {
    session.requireStudent();
    synchronized (session) {
      if (session.cart == null) return Map.of("empty", true);
      String error = "";
      try {
        validateCart(session);
      } catch (BusinessException e) {
        error = e.getMessage();
      }
      return Map.of("empty", false, "plan", session.cart, "valid", error.isEmpty(), "error", error);
    }
  }

  public Object select(LoginSession session, String planId, boolean replace) {
    session.requireStudent();
    synchronized (session) {
      var plan =
          session.mealPlans.stream()
              .filter(p -> planId.equals(p.get("id")))
              .findFirst()
              .orElseThrow(() -> new BusinessException(409, "只能选择本次登录推荐的套餐"));
      nutrition.validatePlan(session, plan);
      if (session.cart != null
          && !"manual".equals(session.cart.get("source"))
          && !session.cart.get("id").equals(planId)
          && !replace) throw new BusinessException(409, "购物车已有套餐，请确认替换");
      if (session.cart != null && session.cart.get("id").equals(planId)) return cart(session);
      session.invalidate("order");
      session.cart = new LinkedHashMap<>(plan);
      session.cart.put("source", "ai");
      session.cart.put("checkoutToken", session.issue("order", planId));
      return cart(session);
    }
  }

  /** 设置指定菜品的绝对份数，0 表示移除；始终使用服务端快照而不是客户端营养数据。 */
  public Object updateDish(LoginSession session, long dishId, int quantity) {
    session.requireStudent();
    BusinessException.require(quantity >= 0 && quantity <= 20, "每道菜最多选择 20 份");
    synchronized (session) {
      var selected = new ArrayList<Map<String, Object>>();
      if (session.cart != null)
        for (var item : (List<Map<String, Object>>) session.cart.get("dishes"))
          selected.add(new LinkedHashMap<>(item));
      var existing =
          selected.stream()
              .filter(item -> ((Number) item.get("id")).longValue() == dishId)
              .findFirst()
              .orElse(null);
      if (quantity == 0) {
        if (existing == null) return cart(session);
        selected.remove(existing);
      } else {
        var available =
            nutrition.availableDishes(session).stream()
                .filter(item -> ((Number) item.get("id")).longValue() == dishId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(409, "菜品已下架或不存在，请刷新菜单"));
        if (existing == null) {
          existing = new LinkedHashMap<>(available);
          selected.add(existing);
        }
        // 数量更新不悄悄换掉已经改变的菜品快照，失效餐仍提示用户重新选择。
        existing.put("quantity", quantity);
      }
      session.invalidate("order");
      if (selected.isEmpty()) {
        session.cart = null;
        return cart(session);
      }
      for (var item : selected) item.putIfAbsent("quantity", 1);
      var totals = new LinkedHashMap<String, Double>();
      for (String field : List.of("energyKcal", "proteinG", "fatG", "carbohydrateG")) {
        double total =
            selected.stream()
                .mapToDouble(
                    item ->
                        ((Number) item.get(field)).doubleValue()
                            * ((Number) item.get("quantity")).intValue())
                .sum();
        totals.put(field, Math.round(total * 10) / 10.0);
      }
      String id = UUID.randomUUID().toString();
      session.cart =
          new LinkedHashMap<>(
              Map.of(
                  "id",
                  id,
                  "source",
                  "manual",
                  "dishes",
                  selected,
                  "totals",
                  totals,
                  "reason",
                  "按你的口味自由搭配"));
      session.cart.put("checkoutToken", session.issue("order", id));
      return cart(session);
    }
  }

  private void validateCart(LoginSession session) {
    if ("manual".equals(session.cart.get("source")))
      nutrition.validateManualPlan(session, session.cart);
    else nutrition.validatePlan(session, session.cart);
  }

  public void clear(LoginSession session) {
    session.requireStudent();
    synchronized (session) {
      session.cart = null;
      session.invalidate("order");
    }
  }

  public Object checkout(LoginSession session, String token) {
    session.requireStudent();
    synchronized (session) {
      var confirmation = session.confirmation(token, "order");
      if (confirmation.result != null) return confirmation.result;
      BusinessException.require(
          session.cart != null && session.cart.get("id").equals(confirmation.payload), "购物车为空或已变更");
      confirmation.result =
          transactions.execute(
              status -> {
                validateCart(session);
                long id =
                    orders.insert(
                        session.userId,
                        CampusTime.now().toString(),
                        (List<Map<String, Object>>) session.cart.get("dishes"));
                return Map.of("id", id, "message", "订单已提交");
              });
      // 仅提交成功清空；供应变化、快照失效或事务失败均保留原购物车。
      session.cart = null;
      return confirmation.result;
    }
  }

  public Object list(LoginSession session, boolean all) {
    if (all) session.requireAdmin();
    else session.requireStudent();
    return orders.list(all ? null : session.userId);
  }
}
