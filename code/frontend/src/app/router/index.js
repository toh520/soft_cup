import { createRouter, createWebHashHistory } from "vue-router";
import { useAuth } from "../../modules/auth/composables/useAuth.js";
import AppLayout from "../layouts/AppLayout.vue";
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/login",
      component: () => import("../../modules/auth/pages/LoginPage.vue"),
    },
    {
      path: "/",
      component: AppLayout,
      children: [
        { path: "", redirect: "/assistant" },
        {
          path: "assistant",
          component: () =>
            import("../../modules/assistant/pages/AssistantPage.vue"),
        },
        {
          path: "knowledge",
          component: () =>
            import("../../modules/knowledge/pages/KnowledgePage.vue"),
        },
        {
          path: "nutrition",
          component: () =>
            import("../../modules/nutrition/pages/NutritionPage.vue"),
        },
        {
          path: "cart",
          redirect: "/nutrition",
        },
        {
          path: "profile",
          component: () =>
            import("../../modules/profile/pages/ProfilePage.vue"),
        },
        {
          path: "admin/:resource",
          component: () => import("../../modules/admin/pages/AdminPage.vue"),
        },
      ],
    },
    { path: "/:pathMatch(.*)*", redirect: "/" },
  ],
});
router.beforeEach(async (to) => {
  if (to.path === "/login") return true;
  const auth = useAuth();
  try {
    if (!auth.user.value) await auth.refresh();
  } catch {
    return "/login";
  }
  if (to.path.startsWith("/admin/") && auth.user.value.role !== "admin")
    return "/assistant";
  if (!to.path.startsWith("/admin/") && auth.user.value.role === "admin")
    return "/admin/students";
});
window.addEventListener("campus:unauthorized", () => {
  useAuth().user.value = null;
  window.dispatchEvent(new Event("campus:logout"));
  router.replace("/login");
});
export default router;
