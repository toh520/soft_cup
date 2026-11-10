<script setup>
import { ref } from "vue";
import { assistantApi } from "../api/assistant-api.js";
defineProps({ books: Array, inactive: Boolean });
const selected = ref(null),
  busy = ref(false),
  error = ref(""),
  success = ref("");
async function select(book) {
  busy.value = true;
  error.value = "";
  try {
    selected.value = await assistantApi.bookDraft(book.id);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function borrow() {
  busy.value = true;
  error.value = "";
  try {
    const result = await assistantApi.borrow(selected.value.token);
    success.value = `${result.message}，可在个人中心归还。`;
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section class="result-card">
    <div v-for="book in books" :key="book.id" class="book-row">
      <div>
        <h3>{{ book.title }}</h3>
        <small>{{ book.author }} · 可借 {{ book.availableCount }} 本</small>
        <p class="muted">{{ book.summary }}</p>
      </div>
      <button
        :disabled="inactive || busy || !book.availableCount || !!success"
        @click="select(book)"
      >
        选择此书
      </button>
    </div>
    <div v-if="selected && !inactive && !success" class="notice">
      <p>确认借阅《{{ selected.book.title }}》？每次借阅一本。</p>
      <button class="primary" :disabled="busy" @click="borrow">确认借书</button>
    </div>
    <p v-if="error" class="notice error">{{ error }}</p>
    <p v-if="success" class="notice success">{{ success }}</p>
    <p v-if="inactive && !success" class="muted">请使用最新查询结果。</p>
  </section>
</template>
