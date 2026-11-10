import { request } from "../../../shared/api/http.js";
export const nutritionApi = {
  dishes: () => request("/nutrition/dishes"),
  readiness: () => request("/nutrition/readiness"),
  recommend: () => request("/nutrition/recommendations", "POST"),
};
