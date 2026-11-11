import { test, expect } from "@playwright/test";
import { loginPage, tabApi } from "./tab-api.js";

test("登录密码切换和键盘可用，桌面手机截图", async ({ page }) => {
  await page.goto("/#/login");
  const password = page.getByLabel("密码", { exact: true });
  await password.fill("sample123");
  await expect(password).toHaveAttribute("type", "password");
  await page.getByRole("button", { name: "显示密码", exact: true }).click();
  await expect(password).toHaveAttribute("type", "text");
  await expect(password).toHaveValue("sample123");
  await page.getByRole("button", { name: "隐藏密码", exact: true }).click();
  await expect(password).toHaveAttribute("type", "password");
  await password.clear();
  await page.screenshot({
    path: "test-results/ui-login-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "test-results/ui-login-mobile.png",
    fullPage: true,
  });
});

test("学生各页面及管理员各入口在桌面平板手机正常，无页面异常", async ({
  page,
  context,
}) => {
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await loginPage(page, "20260001");
  for (const width of [1440, 1024, 768, 390]) {
    await page.setViewportSize({ width, height: 1000 });
    for (const route of ["assistant", "knowledge", "nutrition", "profile"]) {
      await page.goto(`/#/${route}`);
      await expect(page.locator("h1")).toBeVisible();
      if (route === "profile")
        await expect(page.getByLabel("个人资料概览")).toBeVisible();
      if (route === "nutrition")
        await expect(page.locator(".dish-card")).not.toHaveCount(0);
      await expect(page.getByRole("alert")).toHaveCount(0);
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
        `横向溢出：${route} / ${width}`,
      ).toBeTruthy();
      if (width === 1440 || width === 390) {
        await page.screenshot({
          path: `test-results/ui-${route}-${width}.png`,
          fullPage: true,
        });
      }
    }
  }
  const admin = await context.newPage();
  admin.on("pageerror", (error) => errors.push(error.message));
  await loginPage(admin, "admin");
  for (const width of [1440, 390]) {
    await admin.setViewportSize({ width, height: 1000 });
    for (const resource of [
      "students",
      "leave-requests",
      "books",
      "borrow-records",
      "schedules",
      "dishes",
      "knowledge-entries",
      "orders",
    ]) {
      await admin.goto(`/#/admin/${resource}`);
      await expect(admin.locator("h1")).toBeVisible();
      if (width === 390)
        await expect(admin.locator("nav .router-link-active")).toBeInViewport();
      await expect(admin.getByRole("alert")).toHaveCount(0);
      expect(
        await admin.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBeTruthy();
    }
    await admin.goto("/#/admin/dishes");
    await expect(admin.locator("tbody tr")).not.toHaveCount(0);
    await admin.screenshot({
      path: `test-results/ui-admin-${width}.png`,
      fullPage: true,
    });
  }
  expect(errors).toEqual([]);
  await admin.close();
});

test("管理编辑弹窗自动聚焦、Escape取消、恢复焦点及错误保留输入", async ({
  page,
}) => {
  await loginPage(page, "admin");
  await page.goto("/#/admin/books");
  const open = page.getByRole("button", { name: "+ 新增", exact: true });
  await open.click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await expect(dialog.getByLabel("书名", { exact: true })).toBeFocused();
  await page.keyboard.press("Escape");
  await expect(dialog).toHaveCount(0);
  await expect(open).toBeFocused();
  await open.click();
  await dialog.getByLabel("书名", { exact: true }).fill("保留输入验证");
  await dialog.getByLabel("作者", { exact: true }).fill("测试作者");
  await dialog.getByLabel("内容简介").fill("测试内容");
  await page.route("**/api/admin/books", async (route) => {
    if (route.request().method() === "POST")
      await route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ message: "保存失败，请重试" }),
      });
    else await route.continue();
  });
  await dialog.getByRole("button", { name: "保存", exact: true }).click();
  await expect(dialog.getByRole("alert")).toContainText("保存失败");
  await expect(dialog.getByLabel("书名", { exact: true })).toHaveValue(
    "保留输入验证",
  );
  await page.screenshot({
    path: "test-results/ui-editor-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "test-results/ui-editor-mobile.png",
    fullPage: true,
  });
  await dialog.getByRole("button", { name: "取消", exact: true }).click();
  await expect(dialog).toHaveCount(0);
});

test("管理员搜索学生，重置后列表立即显示新密码并可以登录", async ({
  page,
  context,
}) => {
  await loginPage(page, "admin");
  const account = `U${Date.now()}`;
  expect(
    (
      await tabApi(page).post("/api/admin/students", {
        data: {
          name: "交互验证学生",
          studentNumber: account,
          password: "campus123",
        },
      })
    ).ok(),
  ).toBeTruthy();
  await page.getByRole("button", { name: "刷新", exact: true }).click();
  await page.getByLabel("搜索学生").fill(account);
  await expect(page.locator("tbody tr")).toHaveCount(1);
  await page.getByRole("button", { name: "编辑 / 重置密码" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("新密码", { exact: true }).fill("updated123");
  page.once("dialog", (dialog) => dialog.accept());
  await dialog.getByRole("button", { name: "重置密码", exact: true }).click();
  await expect(dialog.getByText("密码已重置", { exact: true })).toBeVisible();
  await dialog.getByRole("button", { name: "关闭编辑窗口" }).click();
  await expect(page.locator("tbody tr")).toContainText("updated123");
  const student = await context.newPage();
  await loginPage(student, account, "updated123");
  await student.close();
});

for (const module of ["assistant", "knowledge"]) {
  test(`${module}失败保留输入且可修改重试`, async ({ page }) => {
    await loginPage(page, "20260001");
    await page.goto(`/#/${module}`);
    const endpoint =
      module === "assistant"
        ? "**/api/assistant/messages"
        : "**/api/knowledge/questions";
    await page.route(endpoint, (route) =>
      route.fulfill({
        status: 504,
        contentType: "application/json",
        body: JSON.stringify({ message: "模型服务超时，请重试" }),
      }),
    );
    const input = page.getByLabel(
      module === "assistant" ? "发送给校园服务助手的消息" : "校园知识问题",
    );
    await input.fill("保留我的问题");
    await page
      .getByRole("button", {
        name: module === "assistant" ? "发送" : "提问",
        exact: true,
      })
      .click();
    await expect(page.getByRole("alert")).toContainText("超时");
    await expect(input).toHaveValue("保留我的问题");
    await expect(input).toBeEnabled();
    await input.fill("修改后重新提问");
  });
}

test("食堂要求快捷填入不会自动调用模型，资料概览保存后同步", async ({
  page,
}) => {
  const account = `P${Date.now()}`;
  await page.request.post("/api/auth/register", {
    data: {
      name: "资料概览测试",
      studentNumber: account,
      password: "campus123",
    },
  });
  await loginPage(page, account, "campus123");
  await page.goto("/#/nutrition");
  let recommendations = 0;
  page.on("request", (request) => {
    if (request.url().includes("/nutrition/recommendations")) recommendations++;
  });
  await page.getByRole("button", { name: "两菜一汤", exact: true }).click();
  await expect(page.getByLabel("本次用餐要求（选填）")).toHaveValue(
    "米饭，两菜一汤，一荤一素",
  );
  expect(recommendations).toBe(0);
  await expect(
    page.getByRole("button", { name: "生成膳食方案", exact: true }),
  ).toBeDisabled();
  await page.goto("/#/profile");
  await expect(page.getByLabel("个人资料概览")).toContainText("0/5");
  await page.getByLabel("年龄（岁）").fill("22");
  await page.getByRole("button", { name: "保存个人资料" }).click();
  await expect(page.getByLabel("个人资料概览")).toContainText("1/5");
});
