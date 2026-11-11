import { sessionGeneration } from "../../../shared/api/tab-session.js";
import { ref } from "vue";
import { askKnowledge, clearKnowledge } from "../api/knowledge-api.js";
// 知识窗口不共享助手的消息、草稿和确认令牌。
const messages = ref([]),
  busy = ref(false),
  error = ref("");
window.addEventListener("campus:logout", () => {
  messages.value = [];
  busy.value = false;
  error.value = "";
});
export function useKnowledgeConversation() {
  async function send(message) {
    if (busy.value || !message.trim()) return;
    const generation = sessionGeneration();
    busy.value = true;
    error.value = "";
    messages.value.push({ role: "user", text: message });
    try {
      const result = await askKnowledge(message);
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
  return {
    messages,
    busy,
    error,
    send,
    clear: async () => {
      const generation = sessionGeneration();
      busy.value = true;
      error.value = "";
      try {
        await clearKnowledge();
        if (generation !== sessionGeneration()) return;
        messages.value = [];
      } catch (e) {
        if (generation === sessionGeneration()) error.value = e.message;
      } finally {
        if (generation === sessionGeneration()) busy.value = false;
      }
    },
  };
}
