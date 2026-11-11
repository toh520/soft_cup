import {
  initializeTabSession,
  sessionGeneration,
  sessionToken,
  setSessionToken,
} from "./tab-session.js";

/** 身份来自当前标签页；旧账号响应不允许覆盖新账号页面或触发新账号退出。 */
export async function request(path, method = "GET", body) {
  await initializeTabSession();
  const generation = sessionGeneration();
  const token = sessionToken();
  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      credentials: "omit",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      ...(method === "GET" ? {} : { body: JSON.stringify(body ?? {}) }),
    });
  } catch {
    throw new Error("网络连接失败，请检查服务后重试");
  }
  const data = await response.json().catch(() => ({}));
  if (generation !== sessionGeneration())
    throw new DOMException("登录状态已变化", "AbortError");
  if (!response.ok) {
    const error = new Error(data.message || "请求失败，请稍后重试");
    error.status = response.status;
    if (response.status === 401 && !path.startsWith("/auth/")) {
      setSessionToken(null);
      window.dispatchEvent(new Event("campus:unauthorized"));
    }
    throw error;
  }
  return data;
}
