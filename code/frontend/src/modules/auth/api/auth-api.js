import { request } from "../../../shared/api/http.js";
import { setSessionToken } from "../../../shared/api/tab-session.js";
export const authApi = {
  login: async (body) => {
    const result = await request("/auth/login", "POST", body);
    setSessionToken(result.token);
    return result;
  },
  register: (body) => request("/auth/register", "POST", body),
  logout: async () => {
    try {
      return await request("/auth/logout", "POST");
    } finally {
      setSessionToken(null);
    }
  },
  profile: () => request("/profile"),
};
