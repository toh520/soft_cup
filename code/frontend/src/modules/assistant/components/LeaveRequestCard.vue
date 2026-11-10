<script setup>
import { reactive, ref } from "vue";
import { assistantApi } from "../api/assistant-api.js";
const props = defineProps({ draft: Object, inactive: Boolean });
const form = reactive({
  startAt: props.draft.startAt?.slice(0, 16) || "",
  endAt: props.draft.endAt?.slice(0, 16) || "",
  reason: props.draft.reason || "",
});
const token = ref(props.draft.token),
  editing = ref(!props.draft.token),
  busy = ref(false),
  error = ref(""),
  success = ref("");
async function edit() {
  busy.value = true;
  try {
    await assistantApi.invalidate();
    token.value = null;
    editing.value = true;
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function preview() {
  busy.value = true;
  error.value = "";
  try {
    const draft = await assistantApi.draft(form);
    token.value = draft.token;
    editing.value = false;
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function submit() {
  busy.value = true;
  error.value = "";
  try {
    const result = await assistantApi.leave(token.value);
    success.value = `申请 #${result.id} 已提交，等待管理员审批。`;
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section class="result-card">
    <div class="card-tag">请假申请 · 待本人确认</div>
    <h3>
      {{ draft.name }} <small>{{ draft.studentNumber }}</small>
    </h3>
    <form @submit.prevent="preview">
      <div class="form-grid">
        <label
          >开始时间<input
            v-model="form.startAt"
            type="datetime-local"
            required
            :disabled="!editing || inactive || busy || !!success" /></label
        ><label
          >结束时间<input
            v-model="form.endAt"
            type="datetime-local"
            required
            :disabled="!editing || inactive || busy || !!success"
        /></label>
      </div>
      <label
        >请假理由<textarea
          v-model="form.reason"
          maxlength="500"
          required
          :disabled="!editing || inactive || busy || !!success"
        ></textarea>
      </label>
      <p v-if="error" class="notice error">{{ error }}</p>
      <p v-if="success" class="notice success">{{ success }}</p>
      <p v-if="inactive && !success" class="muted">
        此卡片已失效，请使用最新回复。
      </p>
      <div v-if="!inactive && !success" class="actions">
        <button v-if="editing" class="primary" :disabled="busy">
          预览并确认内容</button
        ><template v-else
          ><button type="button" :disabled="busy" @click="edit">修改内容</button
          ><button
            type="button"
            class="primary"
            :disabled="busy"
            @click="submit"
          >
            确认提交
          </button></template
        >
      </div>
    </form>
  </section>
</template>
