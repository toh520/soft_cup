<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { authApi } from "../api/auth-api.js";
import { useAuth } from "../composables/useAuth.js";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
import CampusIllustration from "../../../shared/components/CampusIllustration.vue";
const showPassword = ref(false);
const registering = ref(false),
  name = ref(""),
  account = ref(""),
  password = ref(""),
  error = ref(""),
  message = ref(""),
  busy = ref(false);
const router = useRouter();
const auth = useAuth();
async function submit() {
  error.value = "";
  message.value = "";
  busy.value = true;
  try {
    if (registering.value) {
      const result = await authApi.register({
        name: name.value,
        studentNumber: account.value,
        password: password.value,
      });
      message.value = result.message;
      registering.value = false;
      password.value = "";
    } else {
      await authApi.login({ account: account.value, password: password.value });
      window.dispatchEvent(new Event("campus:logout"));
      const user = await auth.refresh();
      router.push(user.role === "admin" ? "/admin/students" : "/assistant");
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <main class="login-shell">
    <section class="login-story">
      <p class="eyebrow">知行校园</p>
      <h1>学习之外，<br />生活也有好帮手。</h1>
      <p>办理校园事务，发现可靠知识，<br />从容安排每一天。</p>
      <CampusIllustration />
      <small>校园服务 · 知识问答 · 营养食堂</small>
    </section>
    <section class="login-panel">
      <div class="login-form">
        <p class="eyebrow">从这里开始你的校园生活</p>
        <h2>{{ registering ? "创建学生账号" : "欢迎回来" }}</h2>
        <p class="muted">
          {{
            registering
              ? "只需姓名、学号和密码，即可开启校园生活。"
              : "使用学号或管理员账号登录。"
          }}
        </p>
        <form @submit.prevent="submit">
          <label v-if="registering"
            >姓名<input
              v-model="name"
              required
              maxlength="50"
              autocomplete="name" /></label
          ><label
            >{{ registering ? "学号" : "学号 / 管理员账号"
            }}<input
              v-model="account"
              required
              maxlength="32"
              :pattern="registering ? '[A-Za-z0-9]{1,32}' : undefined"
              autocomplete="username" /></label
          ><label
            >密码<span class="password-field"
              ><input
                v-model="password"
                aria-label="密码"
                :type="showPassword ? 'text' : 'password'"
                required
                :minlength="registering ? 6 : 1"
                maxlength="64"
                :autocomplete="
                  registering ? 'new-password' : 'current-password'
                "
              /><button
                type="button"
                :aria-pressed="showPassword"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? "隐藏" : "显示" }}
              </button></span
            ></label
          ><FeedbackMessage
            :error="error"
            :message="message"
            :loading="busy"
          /><button class="primary wide" :disabled="busy">
            {{ registering ? "注册学生账号" : "登录校园" }} <span>→</span>
          </button>
        </form>
        <button
          class="text-button switch-login"
          :disabled="busy"
          @click="
            registering = !registering;
            error = '';
            message = '';
          "
        >
          {{ registering ? "已有账号？返回登录" : "还没有账号？注册学生账号" }}
        </button>
        <p class="tab-login-note">每个标签页可独立登录，刷新保留当前账号。</p>
        <p class="login-note">演示系统 · 校园资料与菜品均为课设示例</p>
      </div>
    </section>
  </main>
</template>
