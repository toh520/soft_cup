import { test, expect } from "@playwright/test";
import { loginPage, tabApi } from "./tab-api.js";

test("同一浏览器三标签页账号、刷新、退出和权限互不影响", async ({
  page,
  context,
}) => {
  await loginPage(page, "20260001");
  const second = await context.newPage();
  await second.goto("/");
  await expect(second).toHaveURL(/login/);
  await loginPage(second, "20260002");
  const admin = await context.newPage();
  await loginPage(admin, "admin");
  for (const [tab, account] of [
    [page, "20260001"],
    [second, "20260002"],
    [admin, "admin"],
  ]) {
    await tab.reload();
    await expect(tab.locator(".app-layout, .app-shell").first()).toBeVisible();
    expect(
      (await (await tabApi(tab).get("/api/profile")).json()).studentNumber,
    ).toBe(account);
  }
  expect((await tabApi(second).get("/api/admin/students")).status()).toBe(403);
  await second.getByRole("button", { name: "退出", exact: true }).click();
  await expect(second).toHaveURL(/login/);
  await page.reload();
  expect(
    (await (await tabApi(page).get("/api/profile")).json()).studentNumber,
  ).toBe("20260001");
  expect((await tabApi(admin).get("/api/admin/students")).status()).toBe(200);
  await loginPage(second, "20260003");
  expect(
    (await (await tabApi(page).get("/api/profile")).json()).studentNumber,
  ).toBe("20260001");
  await Promise.all([second.close(), admin.close()]);
});

test("购物车按标签页会话隔离，包括同账号的独立登录", async ({
  page,
  context,
}) => {
  await loginPage(page, "20260001");
  const second = await context.newPage();
  await loginPage(second, "20260001");
  await page.goto("/#/nutrition");
  await second.goto("/#/nutrition");
  await page.getByRole("button", { name: "加入米饭", exact: true }).click();
  await expect(page.locator("#canteen-cart .cart-items li")).toHaveCount(1);
  await expect(second.getByText("这一餐，想吃点什么？")).toBeVisible();
  await page.reload();
  await expect(page.locator("#canteen-cart .cart-items li")).toHaveCount(1);
  await second.getByRole("button", { name: "退出", exact: true }).click();
  expect((await tabApi(page).get("/api/cart")).status()).toBe(200);
  await second.close();
});

test("有 opener 的新标签页和复制存储的标签页不继承账号", async ({
  page,
  context,
}) => {
  await loginPage(page, "20260001");
  const popupPromise = page.waitForEvent("popup");
  await page.evaluate(() => window.open(location.href, "_blank"));
  const popup = await popupPromise;
  await expect(popup).toHaveURL(/login/);
  expect(
    await popup.evaluate(() => sessionStorage.getItem("campus:token")),
  ).toBeNull();
  const snapshot = await page.evaluate(() => ({
    token: sessionStorage.getItem("campus:token"),
    tab: sessionStorage.getItem("campus:tab"),
  }));
  const copied = await context.newPage();
  await copied.goto("/#/login");
  await copied.evaluate(({ token, tab }) => {
    sessionStorage.setItem("campus:token", token);
    sessionStorage.setItem("campus:tab", tab);
  }, snapshot);
  await copied.goto("/#/profile");
  await expect(copied).toHaveURL(/login/);
  await loginPage(popup, "20260002");
  await page.reload();
  expect(
    (await (await tabApi(page).get("/api/profile")).json()).studentNumber,
  ).toBe("20260001");
  await Promise.all([popup.close(), copied.close()]);
});

test("失效凭据只退出当前页，旧 Cookie 不会自动登录", async ({
  page,
  context,
  baseURL,
}) => {
  await loginPage(page, "20260001");
  const second = await context.newPage();
  await loginPage(second, "20260002");
  const token = await page.evaluate(() =>
    sessionStorage.getItem("campus:token"),
  );
  await context.addCookies([
    { name: "CAMPUS_SESSION", value: token, url: baseURL },
  ]);
  const empty = await context.newPage();
  await empty.goto("/");
  await expect(empty).toHaveURL(/login/);
  await tabApi(page).post("/api/auth/logout", { data: {} });
  await page.reload();
  await expect(page).toHaveURL(/login/);
  await second.reload();
  expect(
    (await (await tabApi(second).get("/api/profile")).json()).studentNumber,
  ).toBe("20260002");
  await Promise.all([empty.close(), second.close()]);
});

for (const module of ["assistant", "knowledge"]) {
  for (const responseStatus of [200, 401]) {
    test(`${module} ${responseStatus}旧账号迟到响应不能进入新账号或强制新账号退出`, async ({
      page,
    }) => {
      test.skip(process.env.CAMPUS_MODEL_FIXTURE !== "1", "需要模型协议夹具");
      await loginPage(page, "20260001");
      await page.goto(`/#/${module}`);
      let release, entered;
      const waiting = new Promise((resolve) => {
        release = resolve;
      });
      const started = new Promise((resolve) => {
        entered = resolve;
      });
      const endpoint =
        module === "assistant"
          ? "**/api/assistant/messages"
          : "**/api/knowledge/questions";
      await page.route(endpoint, async (route) => {
        entered();
        await waiting;
        await route.fulfill({
          status: responseStatus,
          contentType: "application/json",
          body: JSON.stringify({
            reply: "旧账号私密内容",
            answer: "旧账号私密内容",
            sources: [],
          }),
        });
      });
      await page
        .getByLabel(
          module === "assistant" ? "发送给校园服务助手的消息" : "校园知识问题",
        )
        .fill("旧账号请求");
      await page
        .getByRole("button", {
          name: module === "assistant" ? "发送" : "提问",
        })
        .click();
      await started;
      await page.getByRole("button", { name: "退出", exact: true }).click();
      await page.getByLabel("学号 / 管理员账号").fill("20260002");
      await page.getByLabel("密码", { exact: true }).fill("123456");
      await page.getByRole("button", { name: "登录校园" }).click();
      await expect(page).toHaveURL(/assistant/);
      await page
        .getByRole("link", {
          name: module === "assistant" ? "校园服务助手" : "校园知识问答",
          exact: true,
        })
        .click();
      const received = page.waitForResponse(endpoint);
      release();
      await (await received).finished();
      await page.evaluate(
        () =>
          new Promise((resolve) =>
            requestAnimationFrame(() => requestAnimationFrame(resolve)),
          ),
      );
      await expect(
        page.getByText("旧账号私密内容", { exact: false }),
      ).toHaveCount(0);
      await expect(page.getByText("旧账号请求", { exact: true })).toHaveCount(
        0,
      );
      await expect(page.getByRole("alert")).toHaveCount(0);
      expect(
        (await (await tabApi(page).get("/api/profile")).json()).studentNumber,
      ).toBe("20260002");
    });
  }
}
