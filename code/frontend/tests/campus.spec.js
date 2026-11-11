import { tabApi } from "./tab-api.js";
import { test, expect } from "@playwright/test";

async function register(page) {
  const account = `B${Date.now()}${Math.floor(Math.random() * 10000)}`;
  await page.goto("/#/login");
  await page.getByRole("button", { name: "还没有账号？注册学生账号" }).click();
  await page.getByLabel("姓名", { exact: true }).fill("浏览器学生");
  await page.getByLabel("学号", { exact: true }).fill(account);
  await page.getByLabel("密码", { exact: true }).fill("campus123");
  await page.getByRole("button", { name: "注册学生账号" }).click();
  await expect(page.getByText("注册成功，请使用学号登录")).toBeVisible();
  await page.getByLabel("学号 / 管理员账号").fill(account);
  await page.getByLabel("密码", { exact: true }).fill("campus123");
  await page.getByRole("button", { name: "登录校园" }).click();
  await expect(
    page.getByRole("heading", { name: "校园服务助手", exact: true }),
  ).toBeVisible();
  return account;
}
async function adminLogin(page) {
  await page.goto("/#/login");
  await page.getByLabel("学号 / 管理员账号").fill("admin");
  await page.getByLabel("密码", { exact: true }).fill("123456");
  await page.getByRole("button", { name: "登录校园" }).click();
  await expect(
    page.getByRole("heading", { name: "学生管理", exact: true }),
  ).toBeVisible();
}
test("学生注册、资料、独立窗口、缺配置反馈和退出", async ({ page }) => {
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await register(page);
  await page.getByRole("link", { name: "校园知识问答" }).click();
  await page.getByLabel("校园知识问题").fill("还没有校园知识吗？");
  await page.getByRole("button", { name: "提问" }).click();
  await expect(page.getByRole("alert")).toContainText("尚未向量化");
  await page.getByRole("link", { name: "校园服务助手" }).click();
  await expect(page.getByText("还没有校园知识吗？")).toHaveCount(0);
  await page.getByLabel("发送给校园服务助手的消息").fill("我想请假");
  await page.getByRole("button", { name: "发送" }).click();
  await expect(page.getByRole("alert")).toContainText("模型服务尚未配置");
  await page.getByRole("link", { name: "智能营养食堂" }).click();
  await expect(
    page.getByText(
      "尚需补全：年龄、生理性别、身高、体重、活动水平。手动点餐可直接使用。",
    ),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "生成膳食方案" }),
  ).toBeDisabled();
  await page.locator("nav").getByRole("link", { name: "个人中心" }).click();
  await page.getByLabel("年龄（岁）").fill("22");
  await page.getByLabel("生理性别").selectOption("male");
  await page.getByLabel("身高（cm）").fill("175");
  await page.getByLabel("体重（kg）").fill("65");
  await page.getByLabel("日常活动水平").selectOption("medium");
  await page.getByRole("button", { name: "保存个人资料" }).click();
  await expect(
    page.getByText("个人资料已保存。可返回营养食堂重新推荐。"),
  ).toBeVisible();
  await page.getByRole("link", { name: "智能营养食堂" }).click();
  await page.getByRole("button", { name: "生成膳食方案" }).click();
  await expect(page.getByRole("alert")).toContainText("模型服务尚未配置");
  await expect(
    page.getByRole("heading", { name: "本餐购物车", exact: true }),
  ).toBeVisible();
  await expect(page.getByText("这一餐，想吃点什么？")).toBeVisible();
  await page.getByRole("button", { name: "退出", exact: true }).click();
  await expect(page.getByRole("heading", { name: "欢迎回来" })).toBeVisible();
  await page.goto("/#/profile");
  await expect(page).toHaveURL(/login/);
  expect(errors).toEqual([]);
});
test("管理数据修改、学生课表同步、借还与审批", async ({ browser }) => {
  const studentContext = await browser.newContext();
  const adminContext = await browser.newContext();
  const student = await studentContext.newPage();
  const admin = await adminContext.newPage();
  const account = await register(student);
  await adminLogin(admin);
  await admin.getByRole("link", { name: "图书管理" }).click();
  await admin.getByRole("button", { name: "+ 新增", exact: true }).click();
  const bookName = `浏览器图书${Date.now()}`;
  await admin.getByLabel("书名", { exact: true }).fill(bookName);
  await admin.getByLabel("作者", { exact: true }).fill("演示作者");
  await admin.getByLabel("内容简介").fill("浏览器验收数据");
  await admin.getByLabel("可借数量", { exact: true }).fill("2");
  await admin.getByRole("button", { name: "保存", exact: true }).click();
  await expect(admin.getByText("数据已保存")).toBeVisible();
  // 通过同一浏览器会话的真实 API 创建待确认内容，UI 验证其落库与后续操作。
  const books = await (
    await tabApi(student).get(
      `/api/books?query=${encodeURIComponent(bookName)}`,
    )
  ).json();
  const draft = await (
    await tabApi(student).post(`/api/books/${books[0].id}/draft`, { data: {} })
  ).json();
  expect(
    (
      await tabApi(student).post("/api/borrow-records/confirm", {
        data: { token: draft.token },
      })
    ).ok(),
  ).toBeTruthy();
  await student.getByRole("link", { name: "个人中心" }).click();
  await student.getByRole("button", { name: "借书记录", exact: true }).click();
  await expect(student.getByText(bookName, { exact: true })).toBeVisible();
  student.once("dialog", (dialog) => dialog.accept());
  await student.getByRole("button", { name: "确认归还" }).click();
  await expect(student.getByText("已归还", { exact: true })).toBeVisible();
  const tomorrow = new Date(Date.now() + 2 * 86400000).toISOString();
  const after = new Date(Date.now() + 3 * 86400000).toISOString();
  const leave = await (
    await tabApi(student).post("/api/leave-requests/draft", {
      data: { startAt: tomorrow, endAt: after, reason: "浏览器审批验证" },
    })
  ).json();
  expect(
    (
      await tabApi(student).post("/api/leave-requests/confirm", {
        data: { token: leave.token },
      })
    ).ok(),
  ).toBeTruthy();
  await admin.getByRole("link", { name: "请假审批" }).click();
  const row = admin.locator(".approval-row").filter({ hasText: account });
  await expect(row).toBeVisible();
  admin.once("dialog", (dialog) => dialog.accept());
  await row.getByRole("button", { name: "通过", exact: true }).click();
  await expect(admin.getByText("审批已完成")).toBeVisible();
  await student.getByRole("button", { name: "刷新", exact: true }).click();
  await student.getByRole("button", { name: "请假记录", exact: true }).click();
  await expect(student.getByText("已通过", { exact: true })).toBeVisible();
  for (const name of [
    "借阅记录",
    "班级课表",
    "食堂菜品",
    "校园知识",
    "订单记录",
  ]) {
    await admin.getByRole("link", { name, exact: false }).click();
    await expect(
      admin.getByRole("heading", { name, exact: true }),
    ).toBeVisible();
    await expect(admin.getByRole("alert")).toHaveCount(0);
  }
  await studentContext.close();
  await adminContext.close();
});
test("窄屏导航和表单无横向溢出", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await register(page);
  for (const name of ["校园知识问答", "智能营养食堂", "个人中心"]) {
    await page.getByRole("link", { name, exact: false }).first().click();
    await expect(page.locator("h1")).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBeTruthy();
  }
  await page.screenshot({
    path: "test-results/mobile-cart.png",
    fullPage: true,
  });
});
