import { request } from "../../../shared/api/http.js";
export const adminApi = {
  list: (resource, query = "") =>
    request(
      `${resource === "books" ? "/books" : `/admin/${resource}`}${query}`,
    ),
  create: (resource, body) => request(`/admin/${resource}`, "POST", body),
  update: (resource, id, body) =>
    request(`/admin/${resource}/${id}`, "PUT", body),
  remove: (resource, item) =>
    request(
      `/admin/${resource}/${item.id}${resource === "knowledge-entries" ? `?revision=${encodeURIComponent(item.revision)}` : ""}`,
      "DELETE",
    ),
  approve: (id, status) =>
    request(`/admin/leave-requests/${id}/approval`, "POST", { status }),
  password: (id, password) =>
    request(`/admin/students/${id}/password`, "POST", { password }),
  rebuild: () => request("/admin/knowledge-entries/rebuild", "POST"),
};
