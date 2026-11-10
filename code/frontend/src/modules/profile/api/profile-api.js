import { request } from "../../../shared/api/http.js";
export const profileApi = {
  get: () => request("/profile"),
  save: (body) => request("/profile", "PUT", body),
  courses: () => request("/schedules"),
  leaves: () => request("/leave-requests"),
  borrows: () => request("/borrow-records"),
  orders: () => request("/orders"),
  returnBook: (id) => request(`/borrow-records/${id}/return`, "POST"),
};
