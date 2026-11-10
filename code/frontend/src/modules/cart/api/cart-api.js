import { request } from "../../../shared/api/http.js";
export const cartApi = {
  quantity: (dishId, quantity) =>
    request(`/cart/items/${dishId}`, "PUT", { quantity }),
  get: () => request("/cart"),
  select: (planId, replace = false) =>
    request("/cart", "PUT", { planId, replace }),
  clear: () => request("/cart", "DELETE"),
  checkout: (token) => request("/orders", "POST", { token }),
};
