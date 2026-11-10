<script setup>
import { ref, nextTick } from "vue";
import PageHeading from "../../../shared/components/PageHeading.vue";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
import CourseTable from "../../../shared/components/CourseTable.vue";
import LeaveRequestCard from "../components/LeaveRequestCard.vue";
import BookBorrowCard from "../components/BookBorrowCard.vue";
import { useAssistantConversation } from "../composables/useAssistantConversation.js";
const { messages, busy, error, send, clear } = useAssistantConversation();
const text = ref(""),
  list = ref(null);
async function submit(value = text.value) {
  if (!value.trim()) return;
  text.value = "";
  await send(value);
  await nextTick();
  list.value?.scrollTo({ top: list.value.scrollHeight, behavior: "smooth" });
}
</script>
<template>
  <PageHeading
    title="校园服务助手"
    description="说出你的需要，剩下的我们一起完成。"
    eyebrow="YOUR CAMPUS COMPANION"
    ><button :disabled="busy" @click="clear">新对话</button></PageHeading
  >
  <section class="chat-panel">
    <div ref="list" class="message-list">
      <div v-if="!messages.length" class="chat-welcome">
        <div class="assistant-symbol">知</div>
        <h2>今天有什么需要帮忙？</h2>
        <p class="muted">
          请假、借书、查课表，从一句话开始。<br />所有申请和借阅，都由你核对后确认。
        </p>
        <div class="suggestions">
          <button @click="submit('我想请假')">我想请假 ↗</button
          ><button @click="submit('查询 Java 程序设计的图书')">
            找一本 Java 图书 ↗</button
          ><button @click="submit('我今天有什么课？')">今天的课表 ↗</button>
        </div>
      </div>
      <article
        v-for="(item, index) in messages"
        :key="index"
        :class="['chat-message', item.role]"
      >
        <div class="message-role">
          {{ item.role === "user" ? "我" : "知行助手" }}
        </div>
        <p class="message-text">{{ item.text || item.reply }}</p>
        <LeaveRequestCard
          v-if="item.draft"
          :draft="item.draft"
          :inactive="item.inactive || busy"
        /><BookBorrowCard
          v-if="item.books?.length"
          :books="item.books"
          :inactive="item.inactive || busy"
        />
        <section v-if="item.schedule" class="result-card">
          <p>{{ item.schedule.message }}</p>
          <CourseTable :courses="item.schedule.courses" />
        </section>
        <RouterLink v-if="item.redirect" class="inline-link" :to="item.redirect"
          >前往对应服务 →</RouterLink
        >
      </article>
      <FeedbackMessage :loading="busy" :error="error" />
    </div>
    <form class="chat-composer" @submit.prevent="submit()">
      <input
        v-model="text"
        aria-label="发送给校园服务助手的消息"
        placeholder="例如：帮我查询明天的课程…"
        required
        maxlength="1000"
        :disabled="busy"
      /><button class="primary" :disabled="busy || !text.trim()">发送 ↑</button>
    </form>
    <p class="composer-note">办理事务请在卡片中确认 · 校园规定请前往知识问答</p>
  </section>
</template>
