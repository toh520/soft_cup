<script setup>
import { ref, onMounted } from "vue";
import { adminApi } from "../api/admin-api.js";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
import { labels } from "../model/resource-schemas.js";
const items = ref([]),
  filter = ref("pending"),
  busy = ref(false),
  error = ref(""),
  message = ref("");
async function load() {
  busy.value = true;
  error.value = "";
  try {
    items.value = await adminApi.list(
      "leave-requests",
      filter.value ? `?status=${filter.value}` : "",
    );
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
async function approve(item, status) {
  if (!confirm(`确认${labels[status]} ${item.name} 的申请？审批后不能更改。`))
    return;
  busy.value = true;
  error.value = "";
  try {
    await adminApi.approve(item.id, status);
    message.value = "审批已完成";
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <FeedbackMessage :loading="busy" :error="error" :message="message" />
  <section class="panel">
    <div class="toolbar">
      <select
        v-model="filter"
        aria-label="审批状态"
        :disabled="busy"
        @change="load"
      >
        <option value="">全部状态</option>
        <option
          v-for="key in ['pending', 'approved', 'rejected']"
          :key="key"
          :value="key"
        >
          {{ labels[key] }}
        </option></select
      ><button :disabled="busy" @click="load">刷新</button>
    </div>
    <div v-if="!items.length" class="empty-state">当前没有相关申请</div>
    <div v-for="item in items" :key="item.id" class="approval-row">
      <div>
        <h3>
          {{ item.name }} <small>{{ item.studentNumber }}</small>
        </h3>
        <p>
          {{ item.startAt.replace("T", " ").slice(0, 16) }} →
          {{ item.endAt.replace("T", " ").slice(0, 16) }}
        </p>
        <p>{{ item.reason }}</p>
      </div>
      <div class="actions">
        <span :class="['badge', item.status]">{{ labels[item.status] }}</span
        ><template v-if="item.status === 'pending'"
          ><button
            class="primary"
            :disabled="busy"
            @click="approve(item, 'approved')"
          >
            通过</button
          ><button :disabled="busy" @click="approve(item, 'rejected')">
            拒绝
          </button></template
        >
      </div>
    </div>
  </section>
</template>
