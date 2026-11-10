<script setup>
import { reactive, ref } from "vue";
import { profileApi } from "../api/profile-api.js";
import { useAuth } from "../../auth/composables/useAuth.js";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
const props = defineProps({ profile: Object });
const { user } = useAuth();
const form = reactive(
  Object.fromEntries(
    [
      "name",
      "age",
      "sex",
      "heightCm",
      "weightKg",
      "activityLevel",
      "dietPreference",
      "avoidedIngredients",
    ].map((key) => [key, props.profile[key]]),
  ),
);
const avoided = ref(form.avoidedIngredients.join("、")),
  busy = ref(false),
  error = ref(""),
  message = ref("");
async function save() {
  busy.value = true;
  error.value = "";
  message.value = "";
  try {
    const body = {
      ...form,
      avoidedIngredients: avoided.value
        .split(/[,，、\n]/)
        .map((s) => s.trim())
        .filter(Boolean),
    };
    for (const field of ["age", "sex", "heightCm", "weightKg", "activityLevel"])
      if (body[field] === "") body[field] = null;
    user.value = await profileApi.save(body);
    message.value = "个人资料已保存。可返回营养食堂重新推荐。";
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section class="panel">
    <div class="section-heading">
      <h2>个人与身体信息</h2>
      <span class="muted"
        >{{ profile.studentNumber }} ·
        {{ profile.className || "未分配班级" }}</span
      >
    </div>
    <form @submit.prevent="save">
      <div class="form-grid three">
        <label>姓名<input v-model="form.name" required maxlength="50" /></label
        ><label
          >年龄（岁）<input
            v-model.number="form.age"
            type="number"
            min="18"
            max="80" /></label
        ><label
          >生理性别<select v-model="form.sex">
            <option :value="null">暂不填写</option>
            <option value="male">男</option>
            <option value="female">女</option>
          </select></label
        ><label
          >身高（cm）<input
            v-model.number="form.heightCm"
            type="number"
            min="100"
            max="250"
            step="0.1" /></label
        ><label
          >体重（kg）<input
            v-model.number="form.weightKg"
            type="number"
            min="25"
            max="250"
            step="0.1" /></label
        ><label
          >日常活动水平<select v-model="form.activityLevel">
            <option :value="null">暂不填写</option>
            <option value="low">低 · 久坐为主</option>
            <option value="medium">中 · 规律活动</option>
            <option value="high">高 · 经常运动</option>
          </select></label
        ><label
          >饮食偏好<select v-model="form.dietPreference">
            <option value="none">不限</option>
            <option value="vegetarian">素食</option>
          </select></label
        ><label class="span-two"
          >需避开的食材<input
            v-model="avoided"
            placeholder="例如：花生、大豆、小麦（用顿号分隔）"
        /></label>
      </div>
      <p class="footnote">
        身体信息可暂不填写，仅生成营养推荐时需要完整。当前估算适用于 18～80
        岁成年人。
      </p>
      <FeedbackMessage :error="error" :message="message" /><button
        class="primary"
        :disabled="busy"
      >
        {{ busy ? "保存中…" : "保存个人资料" }}
      </button>
    </form>
  </section>
</template>
