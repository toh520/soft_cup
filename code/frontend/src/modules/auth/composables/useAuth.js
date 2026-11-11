import { ref } from "vue";
import { authApi } from "../api/auth-api.js";
const user = ref(null);
window.addEventListener("campus:logout", () => {
  user.value = null;
});
export function useAuth() {
  async function refresh() {
    user.value = await authApi.profile();
    return user.value;
  }
  async function logout() {
    try {
      await authApi.logout();
    } finally {
      user.value = null;
    }
  }
  return { user, refresh, logout };
}
