import { ref } from "vue";
import { assistantApi } from "../api/assistant-api.js";
const messages = ref([]),
  busy = ref(false),
  error = ref("");
window.addEventListener("campus:logout", () => {
  messages.value = [];
  error.value = "";
});
export function useAssistantConversation() {
  async function send(text) {
    if (busy.value || !text.trim()) return;
    busy.value = true;
    error.value = "";
    // 新消息使之前卡片停止操作，和服务端的旧令牌失效规则保持一致。
    messages.value.forEach((message) => (message.inactive = true));
    messages.value.push({ role: "user", text });
    try {
      const result = await assistantApi.send(text);
      messages.value.push({ role: "assistant", ...result });
    } catch (e) {
      error.value = e.message;
    } finally {
      busy.value = false;
    }
  }
  async function clear() {
    busy.value = true;
    error.value = "";
    try {
      await assistantApi.clear();
      messages.value = [];
    } catch (e) {
      error.value = e.message;
    } finally {
      busy.value = false;
    }
  }
  return { messages, busy, error, send, clear };
}
