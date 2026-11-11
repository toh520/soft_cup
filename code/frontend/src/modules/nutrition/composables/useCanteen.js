import { computed, onMounted, ref } from "vue";
import { nutritionApi } from "../api/nutrition-api.js";
import { cartApi } from "../../cart/api/cart-api.js";

/** 推荐与购物车独立等待；生成只展示方案，选用才替换服务端购物车。 */
export function useCanteen() {
  const requirements = ref(""),
    readiness = ref(null),
    dishes = ref([]),
    plans = ref([]),
    target = ref(null),
    requirementsSummary = ref(""),
    recommendationNotice = ref(""),
    cart = ref({ empty: true }),
    loading = ref(true),
    recommending = ref(false),
    cartBusy = ref(false),
    error = ref(""),
    cartError = ref(""),
    message = ref("");
  const portions = computed(
    () =>
      cart.value.plan?.dishes.reduce(
        (sum, dish) => sum + (dish.quantity || 1),
        0,
      ) || 0,
  );
  async function load() {
    loading.value = true;
    error.value = "";
    try {
      const result = await Promise.all([
        nutritionApi.readiness(),
        nutritionApi.dishes(),
        cartApi.get(),
      ]);
      [readiness.value, dishes.value, cart.value] = result;
    } catch (e) {
      error.value = e.message;
    } finally {
      loading.value = false;
    }
  }
  onMounted(load);
  async function recommend() {
    if (recommending.value) return;
    recommending.value = true;
    error.value = "";
    plans.value = [];
    requirementsSummary.value = "";
    recommendationNotice.value = "";
    try {
      readiness.value = await nutritionApi.readiness();
      if (!readiness.value.ready) return;
      const result = await nutritionApi.recommend(requirements.value.trim());
      plans.value = result.plans;
      target.value = result.targetEnergyKcal;
      requirementsSummary.value = result.requirementsSummary;
      recommendationNotice.value = result.notice;
    } catch (e) {
      error.value = e.message;
    } finally {
      recommending.value = false;
    }
  }
  async function update(action, success = "") {
    if (cartBusy.value) return;
    cartBusy.value = true;
    cartError.value = "";
    message.value = "";
    try {
      cart.value = await action();
      message.value = success;
    } catch (e) {
      cartError.value = e.message;
      try {
        cart.value = await cartApi.get();
      } catch {
        /* 保留当前展示，原错误用于重试反馈。 */
      }
    } finally {
      cartBusy.value = false;
    }
  }
  function select(plan) {
    return update(
      () => cartApi.select(plan.id, true),
      "已选用 AI 套餐，购物车已替换。",
    );
  }
  function quantity(id, count) {
    return update(() => cartApi.quantity(id, count));
  }
  function add(dish) {
    const existing = cart.value.plan?.dishes.find(
      (item) => item.id === dish.id,
    );
    return quantity(dish.id, (existing?.quantity || (existing ? 1 : 0)) + 1);
  }
  function clear() {
    return update(async () => {
      await cartApi.clear();
      return { empty: true };
    });
  }
  // 提交成功提示需与清空动作一同保留。
  async function submitOrder() {
    if (cartBusy.value || cart.value.empty) return;
    cartBusy.value = true;
    cartError.value = "";
    message.value = "";
    try {
      const result = await cartApi.checkout(cart.value.plan.checkoutToken);
      cart.value = { empty: true };
      message.value = `订单 #${result.id} 已提交，可在个人中心查看。`;
    } catch (e) {
      cartError.value = e.message;
      try {
        cart.value = await cartApi.get();
      } catch {
        /* 保留原购物车以便重试。 */
      }
    } finally {
      cartBusy.value = false;
    }
  }
  return {
    requirements,
    readiness,
    dishes,
    plans,
    target,
    requirementsSummary,
    recommendationNotice,
    cart,
    loading,
    recommending,
    cartBusy,
    error,
    cartError,
    message,
    portions,
    load,
    recommend,
    select,
    quantity,
    add,
    clear,
    checkout: submitOrder,
  };
}
