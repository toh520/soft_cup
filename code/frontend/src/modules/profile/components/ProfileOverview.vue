<script setup>
import { computed } from "vue";
import AppIcon from "../../../shared/components/AppIcon.vue";
const props = defineProps({ profile: Object });
const fields = ["age", "sex", "heightCm", "weightKg", "activityLevel"];
const completed = computed(
  () => fields.filter((field) => props.profile[field] != null).length,
);
</script>
<template>
  <section class="profile-overview" aria-label="个人资料概览">
    <div class="profile-identity">
      <span class="profile-avatar">{{ profile.name?.slice(0, 1) }}</span>
      <div>
        <h2>{{ profile.name }}</h2>
        <p>
          {{ profile.studentNumber
          }}<span>{{ profile.className || "未分配班级" }}</span>
        </p>
      </div>
    </div>
    <div class="profile-readiness">
      <AppIcon name="leaf" :size="22" />
      <div>
        <strong>{{
          completed === 5
            ? "营养推荐资料已就绪"
            : `身体资料已填写 ${completed}/5 项`
        }}</strong>
        <p>
          {{
            completed === 5
              ? "可以到食堂定制这一餐。"
              : "补全下方资料，即可使用 AI 配餐。"
          }}
        </p>
      </div>
      <RouterLink to="/nutrition">前往食堂</RouterLink>
    </div>
  </section>
</template>
