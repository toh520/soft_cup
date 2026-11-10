import { ref } from "vue";
import { askKnowledge, clearKnowledge } from "../api/knowledge-api.js";
// 知识窗口不共享助手的消息、草稿和确认令牌。
const messages = ref([]),
  busy = ref(false),
  error = ref("");
window.addEventListener("campus:logout", () => {
  messages.value = [];
  error.value = "";
});
export function useKnowledgeConversation() {
  async function send(message) {
    if (busy.value || !message.trim()) return;
    busy.value = true;
    error.value = "";
    messages.value.push({ role: "user", text: message });
    try {
      messages.value.push({
        role: "assistant",
        ...(await askKnowledge(message)),
      });
    } catch (e) {
      error.value = e.message;
    } finally {
      busy.value = false;
    }
  }
  return {
    messages,
    busy,
    error,
    send,
    clear: async () => {
      busy.value = true;
      error.value = "";
      try {
        await clearKnowledge();
        messages.value = [];
      } catch (e) {
        error.value = e.message;
      } finally {
        busy.value = false;
      }
    },
  };
}
