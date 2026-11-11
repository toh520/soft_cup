const tokenKey = "campus:token";
const tabKey = "campus:tab";
let generation = 0;
let ready;
window.addEventListener("pageshow", (event) => {
  if (event.persisted) window.location.reload();
});

export const sessionGeneration = () => generation;
export const sessionToken = () => sessionStorage.getItem(tokenKey);

/** 切换账号先作废旧请求的结果；凭据只保存在当前标签页，刷新仍能恢复。 */
export function setSessionToken(token) {
  generation++;
  if (token) sessionStorage.setItem(tokenKey, token);
  else sessionStorage.removeItem(tokenKey);
  window.dispatchEvent(new Event("campus:logout"));
}

/** 新标签页可能继承 opener 的 sessionStorage，独占锁阻止复制出来的凭据继续使用。 */
export function initializeTabSession() {
  return (ready ??= new Promise((resolve, reject) => {
    if (!navigator.locks) {
      reject(
        new Error(
          "当前浏览器不支持标签页独立登录，请使用新版 Chrome 或 Edge，并通过本机地址或 HTTPS 访问",
        ),
      );
      return;
    }
    let id = sessionStorage.getItem(tabKey) || crypto.randomUUID();
    sessionStorage.setItem(tabKey, id);
    const claim = () => {
      navigator.locks
        .request(`campus-tab:${id}`, { ifAvailable: true }, (lock) => {
          if (!lock) {
            sessionStorage.removeItem(tokenKey);
            id = crypto.randomUUID();
            sessionStorage.setItem(tabKey, id);
            claim();
            return;
          }
          resolve();
          // 页面离开时释放锁；浏览器关闭/刷新也会自动释放，不影响其他标签页。
          return new Promise((release) =>
            window.addEventListener("pagehide", release, { once: true }),
          );
        })
        .catch(reject);
    };
    claim();
  }));
}
