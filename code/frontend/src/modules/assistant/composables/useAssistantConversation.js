import { sessionGeneration } from "../../../shared/api/tab-session.js";
import { ref } from "vue";
import { assistantApi } from "../api/assistant-api.js";
const messages = ref([]),
  busy = ref(false),
  error = ref("");
window.addEventListener("campus:logout", () => {
  messages.value = [];
  busy.value = false;
  error.value = "";
});
export function useAssistantConversation() {
  async function send(text) {
    if (busy.value || !text.trim()) return;
    const generation = sessionGeneration();
    busy.value = true;
    error.value = "";
    // 新消息使之前卡片停止操作，和服务端的旧令牌失效规则保持一致。
    messages.value.forEach((message) => (message.inactive = true));
    messages.value.push({ role: "user", text });
    try {
      const result = await assistantApi.send(text);
      if (generation !== sessionGeneration()) return;
      messages.value.push({ role: "assistant", ...result });
      return true;
    } catch (e) {
      if (generation === sessionGeneration()) error.value = e.message;
      return false;
    } finally {
      if (generation === sessionGeneration()) busy.value = false;
    }
  }
  async function clear() {
    const generation = sessionGeneration();
    busy.value = true;
    error.value = "";
    try {
      await assistantApi.clear();
      if (generation !== sessionGeneration()) return;
      messages.value = [];
    } catch (e) {
      if (generation === sessionGeneration()) error.value = e.message;
    } finally {
      if (generation === sessionGeneration()) busy.value = false;
    }
  }
  return { messages, busy, error, send, clear };
}
