import { ref } from "vue";
import { authApi } from "../api/auth-api.js";
const user = ref(null);
export function useAuth() {
  async function refresh() {
    user.value = await authApi.profile();
    return user.value;
  }
  async function logout() {
    await authApi.logout();
    user.value = null;
    window.dispatchEvent(new Event("campus:logout"));
  }
  return { user, refresh, logout };
}
