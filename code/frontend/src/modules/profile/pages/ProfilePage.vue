<script setup>
import { ref, onMounted } from "vue";
import { profileApi } from "../api/profile-api.js";
import PageHeading from "../../../shared/components/PageHeading.vue";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
import ProfileForm from "../components/ProfileForm.vue";
import PersonalRecords from "../components/PersonalRecords.vue";
import ProfileOverview from "../components/ProfileOverview.vue";
const profile = ref(null),
  error = ref(""),
  busy = ref(true);
onMounted(async () => {
  try {
    profile.value = await profileApi.get();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
});
</script>
<template>
  <PageHeading
    title="个人中心"
    description="照顾好自己，也把校园生活安排妥当。"
    eyebrow="MY CAMPUS LIFE"
  /><FeedbackMessage :loading="busy" :error="error" />
  <ProfileOverview v-if="profile" :profile="profile" />
  <div class="stack">
    <ProfileForm
      v-if="profile"
      :profile="profile"
      @saved="profile = $event"
    /><PersonalRecords v-if="profile" />
  </div>
</template>
