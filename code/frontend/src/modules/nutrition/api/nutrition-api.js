import { request } from "../../../shared/api/http.js";
export const nutritionApi = {
  dishes: () => request("/nutrition/dishes"),
  readiness: () => request("/nutrition/readiness"),
  recommend: (requirements = "") =>
    request("/nutrition/recommendations", "POST", { requirements }),
};
