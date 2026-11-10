/** Cookie 由浏览器管理；错误统一转换为中文业务提示，身份判断仍由后端执行。 */
export async function request(path, method = "GET", body) {
  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      ...(method === "GET" ? {} : { body: JSON.stringify(body ?? {}) }),
    });
  } catch {
    throw new Error("网络连接失败，请检查服务后重试");
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(data.message || "请求失败，请稍后重试");
    error.status = response.status;
    if (response.status === 401 && !path.startsWith("/auth/"))
      window.dispatchEvent(new Event("campus:unauthorized"));
    throw error;
  }
  return data;
}
