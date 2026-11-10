<script setup>
import { ref, onMounted } from "vue";
import { adminApi } from "../api/admin-api.js";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
const props = defineProps({ resource: String });
const items = ref([]),
  busy = ref(false),
  error = ref("");
async function load() {
  busy.value = true;
  error.value = "";
  try {
    items.value = await adminApi.list(props.resource);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
const time = (value) => value?.replace("T", " ").slice(0, 16);
</script>
<template>
  <FeedbackMessage :loading="busy" :error="error" />
  <section class="panel">
    <div class="toolbar">
      <button :disabled="busy" @click="load">刷新</button>
    </div>
    <div v-if="!items.length" class="empty-state">暂无业务记录</div>
    <div v-for="item in items" :key="item.id" class="record-row">
      <div>
        <strong>{{ item.name }} · {{ item.studentNumber }}</strong>
        <p v-if="resource === 'orders'">
          订单 #{{ item.id }} ·
          {{
            item.items
              .map((dish) => `${dish.dishName} ×${dish.quantity || 1}`)
              .join(" / ")
          }}
        </p>
        <p v-else>
          《{{ item.title }}》 · {{ item.returnedAt ? "已归还" : "借阅中" }}
        </p>
        <small v-if="item.returnedAt"
          >归还时间：{{ time(item.returnedAt) }}</small
        >
      </div>
      <span class="muted">{{ time(item.orderedAt || item.borrowedAt) }}</span>
    </div>
  </section>
</template>
