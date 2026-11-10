import { request } from "../../../shared/api/http.js";
export const assistantApi = {
  send: (message) => request("/assistant/messages", "POST", { message }),
  clear: () => request("/assistant/conversation", "DELETE"),
  draft: (body) => request("/leave-requests/draft", "POST", body),
  invalidate: () => request("/leave-requests/invalidate", "POST"),
  leave: (token) => request("/leave-requests/confirm", "POST", { token }),
  bookDraft: (id) => request(`/books/${id}/draft`, "POST"),
  borrow: (token) => request("/borrow-records/confirm", "POST", { token }),
};
