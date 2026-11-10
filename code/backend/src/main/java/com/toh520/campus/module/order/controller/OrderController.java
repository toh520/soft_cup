package com.toh520.campus.module.order.controller;

import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.order.dto.OrderRequests;
import com.toh520.campus.module.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {
  private final OrderService orders;

  public OrderController(OrderService orders) {
    this.orders = orders;
  }

  @GetMapping("/cart")
  Object cart(@RequestAttribute LoginSession session) {
    return orders.cart(session);
  }

  @PutMapping("/cart")
  Object select(
      @RequestAttribute LoginSession session, @Valid @RequestBody OrderRequests.Selection request) {
    return orders.select(session, request.planId(), request.replace());
  }

  @DeleteMapping("/cart")
  Object clear(@RequestAttribute LoginSession session) {
    orders.clear(session);
    return Map.of("message", "购物车已清空");
  }

  @PutMapping("/cart/items/{dishId}")
  Object updateDish(
      @RequestAttribute LoginSession session,
      @PathVariable long dishId,
      @Valid @RequestBody OrderRequests.DishQuantity request) {
    return orders.updateDish(session, dishId, request.quantity());
  }

  @PostMapping("/orders")
  Object checkout(
      @RequestAttribute LoginSession session, @Valid @RequestBody OrderRequests.Confirm request) {
    return orders.checkout(session, request.token());
  }

  @GetMapping("/orders")
  Object mine(@RequestAttribute LoginSession session) {
    return orders.list(session, false);
  }

  @GetMapping("/admin/orders")
  Object all(@RequestAttribute LoginSession session) {
    return orders.list(session, true);
  }
}
