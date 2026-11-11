<script setup>
import { computed, ref, watch, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth } from "../../modules/auth/composables/useAuth.js";
import AppIcon from "../../shared/components/AppIcon.vue";
const { user, logout } = useAuth();
const route = useRoute(),
  router = useRouter();
const links = computed(() =>
  user.value?.role === "admin"
    ? [
        ["students", "学生管理", "profile"],
        ["leave-requests", "请假审批", "check"],
        ["books", "图书管理", "knowledge"],
        ["borrow-records", "借阅记录", "knowledge"],
        ["schedules", "班级课表", "calendar"],
        ["dishes", "食堂菜品", "nutrition"],
        ["knowledge-entries", "校园知识", "assistant"],
        ["orders", "订单记录", "cart"],
      ].map(([path, label, icon]) => [`/admin/${path}`, label, icon])
    : [
        ["/assistant", "校园服务助手", "assistant"],
        ["/knowledge", "校园知识问答", "knowledge"],
        ["/nutrition", "智能营养食堂", "nutrition"],
        ["/profile", "个人中心", "profile"],
      ],
);
const navigation = ref(null);
// 只滚动导航容器，让手机上当前入口可见，不改变正文的滚动位置。
watch(
  () => route.path,
  async () => {
    await nextTick();
    const active = navigation.value?.querySelector(".router-link-active");
    if (active && navigation.value.scrollWidth > navigation.value.clientWidth) {
      navigation.value.scrollTo({
        left: active.offsetLeft - navigation.value.offsetLeft - 12,
        behavior: "instant",
      });
    }
  },
  { immediate: true },
);
const currentLabel = computed(
  () => links.value.find(([path]) => path === route.path)?.[1] || "校园服务",
);
function focusContent() {
  document.getElementById("main-content")?.focus();
}
const date = new Intl.DateTimeFormat("zh-CN", {
  timeZone: "Asia/Shanghai",
  month: "long",
  day: "numeric",
  weekday: "long",
}).format(new Date());
async function signOut() {
  try {
    await logout();
  } catch (error) {
    alert(error.message);
  } finally {
    router.push("/login");
  }
}
</script>
<template>
  <div class="app-shell">
    <a href="#main-content" class="skip-link" @click.prevent="focusContent"
      >跳到主要内容</a
    >
    <aside class="sidebar">
      <RouterLink class="brand" to="/"
        ><span class="brand-symbol"
          ><AppIcon name="knowledge" :size="25" /></span
        ><span class="brand-name"
          >知行<small>你的校园生活指南</small></span
        ></RouterLink
      >
      <div class="sidebar-label">
        {{ user?.role === "admin" ? "校园管理" : "校园生活" }}
      </div>
      <nav ref="navigation" aria-label="主导航">
        <RouterLink v-for="[path, label, icon] in links" :key="path" :to="path"
          ><AppIcon :name="icon" :size="19" /><span>{{ label }}</span
          ><span class="nav-active-dot"></span
        ></RouterLink>
      </nav>
      <div class="sidebar-note">
        <AppIcon name="leaf" :size="23" />
        <p>照顾好每一天，<br />从校园里的小事开始。</p>
      </div>
      <div class="sidebar-bottom">
        <div class="avatar">{{ user?.name?.slice(0, 1) }}</div>
        <div class="sidebar-user">
          <strong>{{ user?.name }}</strong
          ><small>{{
            user?.role === "admin" ? "校园管理员" : user?.studentNumber
          }}</small>
        </div>
        <button
          class="text-button logout-button"
          aria-label="退出"
          title="退出登录"
          @click="signOut"
        >
          <AppIcon name="logout" :size="17" />
        </button>
      </div>
    </aside>
    <main
      id="main-content"
      tabindex="-1"
      :class="[
        'workspace',
        { 'workspace-canteen': route.path === '/nutrition' },
      ]"
    >
      <header class="topbar">
        <div class="breadcrumb">
          知行校园<span>/</span><strong>{{ currentLabel }}</strong>
        </div>
        <div class="topbar-date">
          <AppIcon name="calendar" :size="15" /><span>{{ date }}</span>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>
