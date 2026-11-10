import { request } from "../../../shared/api/http.js";
export const askKnowledge = (message) =>
  request("/knowledge/questions", "POST", { message });

export const clearKnowledge = () =>
  request("/knowledge/conversation", "DELETE");
