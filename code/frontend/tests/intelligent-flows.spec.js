import { test, expect } from "@playwright/test";
// 此组需显式启用测试模型的后端；普通未配置模型的演示服务不会运行它。
test.skip(process.env.CAMPUS_MODEL_FIXTURE !== "1", "需要独立测试模型协议夹具");
async function login(page) {
  const account = `A${Date.now()}`;
  await page.request.post("/api/auth/register", {
    data: {
      name: "智能流程学生",
      studentNumber: account,
      password: "campus123",
    },
  });
  await page.request.post("/api/auth/login", {
    data: { account, password: "campus123" },
  });
  await page.goto("/#/assistant");
  await expect(
    page.getByRole("heading", { name: "校园服务助手", exact: true }),
  ).toBeVisible();
  return account;
}
test("完整对话卡片、显式确认、购物车订单与真实向量协议", async ({
  page,
  browser,
}) => {
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await login(page);
  const input = page.getByLabel("发送给校园服务助手的消息");
  await input.fill("我想请假");
  await page.getByRole("button", { name: "发送" }).click();
  await expect(page.getByText("请补充具体起止时间和理由。")).toBeVisible();
  expect(
    await (await page.request.get("/api/leave-requests")).json(),
  ).toHaveLength(0);
  await input.fill("补充时间和理由");
  await page.getByRole("button", { name: "发送" }).click();
  await expect(
    page.getByRole("button", { name: "确认提交", exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel("开始时间").last()).not.toHaveValue("");
  await page.getByRole("button", { name: "修改内容", exact: true }).click();
  await page.getByLabel("请假理由").last().fill("浏览器修改后的请假原因");
  await page.getByRole("button", { name: "预览并确认内容" }).last().click();
  await page.getByRole("button", { name: "确认提交", exact: true }).click();
  await expect(page.getByText(/已提交，等待管理员审批/)).toBeVisible();
  expect(
    await (await page.request.get("/api/leave-requests")).json(),
  ).toHaveLength(1);
  await input.fill("查询Java图书");
  await page.getByRole("button", { name: "发送" }).click();
  await page.getByRole("button", { name: "选择此书" }).first().click();
  await page.getByRole("button", { name: "确认借书", exact: true }).click();
  await expect(page.getByText("借书成功，可在个人中心归还。")).toBeVisible();
  await page.request.put("/api/profile", {
    data: {
      name: "智能流程学生",
      age: 22,
      sex: "male",
      heightCm: 175,
      weightKg: 65,
      activityLevel: "medium",
      dietPreference: "none",
      avoidedIngredients: [],
    },
  });
  await page.getByRole("link", { name: "智能营养食堂" }).click();
  await page.getByRole("button", { name: "生成三个膳食方案" }).click();
  await expect(page.locator(".meal-card")).toHaveCount(3);
  await page.getByRole("button", { name: "选用套餐" }).first().click();
  await expect(
    page.getByRole("heading", { name: "本餐购物车", exact: true }),
  ).toBeVisible();
  expect(await (await page.request.get("/api/orders")).json()).toHaveLength(0);
  await page.getByRole("button", { name: "提交订单" }).click();
  await expect(page.getByText(/订单 #\d+ 已提交/)).toBeVisible();
  expect(await (await page.request.get("/api/orders")).json()).toHaveLength(1);
  const admin = await browser.newContext();
  const adminPage = await admin.newPage();
  await adminPage.request.post("/api/auth/login", {
    data: { account: "admin", password: "123456" },
  });
  await adminPage.goto("/#/admin/knowledge-entries");
  await adminPage.getByRole("button", { name: "+ 新增", exact: true }).click();
  await adminPage.getByLabel("资料标题").fill("浏览器RAG验收");
  await adminPage
    .getByLabel("资料正文（不超过 4000 字）")
    .fill("宿舍电器最大功率为300W。");
  await adminPage.getByRole("button", { name: "保存", exact: true }).click();
  await expect(adminPage.getByText("数据已保存")).toBeVisible();
  adminPage.once("dialog", (dialog) => dialog.accept());
  await adminPage
    .getByRole("button", { name: "重建全部向量", exact: true })
    .click();
  await expect(adminPage.getByText("向量已全部重建")).toBeVisible();
  await page.getByRole("link", { name: "校园知识问答" }).click();
  await page.getByLabel("校园知识问题").fill("宿舍限电多少瓦？");
  await page.getByRole("button", { name: "提问" }).click();
  await expect(page.locator("blockquote")).toContainText("300W");
  await expect(page.locator(".message-text").last()).toContainText("300W");
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: "instant" }));
  await page.screenshot({
    path: "test-results/knowledge-desktop.png",
    fullPage: true,
  });
  expect(errors).toEqual([]);
  await admin.close();
});
