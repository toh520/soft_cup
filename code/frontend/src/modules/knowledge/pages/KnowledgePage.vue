<script setup>
import { ref, nextTick } from "vue";
import PageHeading from "../../../shared/components/PageHeading.vue";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
import { useKnowledgeConversation } from "../composables/useKnowledgeConversation.js";
const { messages, busy, error, send, clear } = useKnowledgeConversation();
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
    title="校园知识问答"
    description="从校园资料中寻找答案，每一条回答都有据可查。"
    eyebrow="ANSWERS WITH SOURCES"
    ><button :disabled="busy" @click="clear">新对话</button></PageHeading
  >
  <section class="chat-panel">
    <div ref="list" class="message-list">
      <div v-if="!messages.length" class="chat-welcome">
        <div class="assistant-symbol knowledge-symbol">问</div>
        <h2>关于校园，你想了解什么？</h2>
        <p class="muted">
          规章制度、课程介绍、活动资讯。<br />资料里没有的内容，会明确告诉你。
        </p>
        <div class="suggestions">
          <button @click="submit('宿舍限电多少瓦？')">宿舍用电规定 ↗</button
          ><button @click="submit('书能借多久？')">图书借阅期限 ↗</button>
        </div>
      </div>
      <article
        v-for="(item, index) in messages"
        :key="index"
        :class="['chat-message', item.role]"
      >
        <div class="message-role">
          {{ item.role === "user" ? "我" : "校园知识" }}
        </div>
        <p class="message-text">{{ item.text || item.answer }}</p>
        <blockquote
          v-for="source in item.sources"
          :key="source.id + source.quote"
        >
          <small>资料来源 · {{ source.title }}</small>
          <p>{{ source.quote }}</p>
        </blockquote>
        <RouterLink v-if="item.redirect" :to="item.redirect" class="inline-link"
          >前往校园服务助手 →</RouterLink
        >
      </article>
      <FeedbackMessage :loading="busy" :error="error" />
    </div>
    <form class="chat-composer" @submit.prevent="submit()">
      <input
        v-model="text"
        aria-label="校园知识问题"
        placeholder="询问校园规定、课程介绍或活动…"
        required
        maxlength="1000"
        :disabled="busy"
      /><button class="primary" :disabled="busy || !text.trim()">提问 ↑</button>
    </form>
    <p class="composer-note">仅依据管理员维护的资料回答 · 此窗口不办理业务</p>
  </section>
</template>
