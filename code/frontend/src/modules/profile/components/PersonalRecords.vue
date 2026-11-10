<script setup>
import { ref, onMounted } from "vue";
import { profileApi } from "../api/profile-api.js";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
import CourseTable from "../../../shared/components/CourseTable.vue";
const tab = ref("courses"),
  data = ref({ courses: [], leaves: [], borrows: [], orders: [] }),
  busy = ref(false),
  error = ref(""),
  scheduleMessage = ref("");
const status = { pending: "待审批", approved: "已通过", rejected: "已拒绝" };
async function load() {
  busy.value = true;
  error.value = "";
  try {
    const results = await Promise.all([
      profileApi.courses(),
      profileApi.leaves(),
      profileApi.borrows(),
      profileApi.orders(),
    ]);
    data.value = {
      courses: results[0].courses,
      leaves: results[1],
      borrows: results[2],
      orders: results[3],
    };
    scheduleMessage.value = results[0].message;
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
async function returnBook(record) {
  if (!confirm(`确认归还《${record.title}》？`)) return;
  busy.value = true;
  error.value = "";
  try {
    await profileApi.returnBook(record.id);
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
const time = (value) => value?.replace("T", " ").slice(0, 16);
</script>
<template>
  <section class="panel">
    <div class="section-heading">
      <h2>我的校园记录</h2>
      <button :disabled="busy" @click="load">刷新</button>
    </div>
    <div class="tabs">
      <button
        v-for="(label, key) in {
          courses: '我的课表',
          leaves: '请假记录',
          borrows: '借书记录',
          orders: '订单记录',
        }"
        :key="key"
        :class="{ active: tab === key }"
        @click="tab = key"
      >
        {{ label }}
      </button>
    </div>
    <FeedbackMessage :loading="busy" :error="error" /><template
      v-if="tab === 'courses'"
      ><p class="muted">{{ scheduleMessage }}</p>
      <CourseTable :courses="data.courses"
    /></template>
    <div v-else-if="!data[tab].length" class="empty-state">还没有相关记录</div>
    <div v-else class="record-list">
      <article v-for="record in data[tab]" :key="record.id" class="record-row">
        <template v-if="tab === 'leaves'"
          ><div>
            <strong
              >{{ time(record.startAt) }} → {{ time(record.endAt) }}</strong
            >
            <p>{{ record.reason }}</p>
          </div>
          <span :class="['badge', record.status]">{{
            status[record.status]
          }}</span></template
        ><template v-if="tab === 'borrows'"
          ><div>
            <strong>{{ record.title }}</strong>
            <p class="muted">
              借阅：{{ time(record.borrowedAt)
              }}<span v-if="record.returnedAt">
                · 归还：{{ time(record.returnedAt) }}</span
              >
            </p>
          </div>
          <button
            v-if="!record.returnedAt"
            :disabled="busy"
            @click="returnBook(record)"
          >
            确认归还</button
          ><span v-else class="badge">已归还</span></template
        ><template v-if="tab === 'orders'"
          ><div>
            <strong>订单 #{{ record.id }}</strong>
            <p>
              {{
                record.items
                  .map((item) => `${item.dishName} ×${item.quantity || 1}`)
                  .join(" · ")
              }}
            </p>
            <small
              >{{
                record.items.reduce(
                  (sum, item) => sum + item.energyKcal * (item.quantity || 1),
                  0,
                )
              }}
              kcal</small
            >
          </div>
          <span class="muted">{{ time(record.orderedAt) }}</span></template
        >
      </article>
    </div>
  </section>
</template>
