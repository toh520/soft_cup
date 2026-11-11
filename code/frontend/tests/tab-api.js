/** API 验证显式使用被测标签页凭据，不能使用浏览器上下文共享的 Cookie。 */
export function tabApi(page) {
  return Object.fromEntries(
    ["get", "post", "put", "delete"].map((method) => [
      method,
      async (url, options = {}) => {
        const token = await page
          .evaluate(() => sessionStorage.getItem("campus:token"))
          .catch(() => null);
        const response = await page.request[method](url, {
          ...options,
          headers: {
            ...options.headers,
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        });
        if (url === "/api/auth/login" && response.ok()) {
          const { token: newToken } = await response.json();
          if (!page.url().startsWith("http")) await page.goto("/#/login");
          await page.evaluate(
            (value) => sessionStorage.setItem("campus:token", value),
            newToken,
          );
        }
        return response;
      },
    ]),
  );
}

export async function loginPage(page, account, password = "123456") {
  await page.goto("/#/login");
  await page.getByLabel("学号 / 管理员账号").fill(account);
  await page.getByLabel("密码", { exact: true }).fill(password);
  await page.getByRole("button", { name: "登录校园" }).click();
  await page.waitForURL(account === "admin" ? /admin\/students/ : /assistant/);
}
