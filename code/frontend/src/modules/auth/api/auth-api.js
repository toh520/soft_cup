import { request } from "../../../shared/api/http.js";
export const authApi = {
  login: (body) => request("/auth/login", "POST", body),
  register: (body) => request("/auth/register", "POST", body),
  logout: () => request("/auth/logout", "POST"),
  profile: () => request("/profile"),
};
