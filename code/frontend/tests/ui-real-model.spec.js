import { test, expect } from "@playwright/test";
import { loginPage } from "./tab-api.js";

// 仅对配置真实模型的独立数据库服务运行，不修改正式业务记录。
test("新版UI真实助手查书、RAG引用和本餐要求推荐", async ({ page }) => {
  test.skip(process.env.CAMPUS_REAL_UI !== "1", "需要真实模型隔离数据库服务");
  test.setTimeout(240000);
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await loginPage(page, "20260001");
  await page
    .getByLabel("发送给校园服务助手的消息")
    .fill("查询Java程序设计的图书");
  await page.getByRole("button", { name: "发送", exact: true }).click();
  await expect(page.locator(".book-row")).not.toHaveCount(0, {
    timeout: 90000,
  });
  await expect(page.locator(".book-row").first()).toContainText("Java");
  await page.getByRole("link", { name: "校园知识问答", exact: true }).click();
  await page.getByLabel("校园知识问题").fill("宿舍限电多少瓦？");
  await page.getByRole("button", { name: "提问", exact: true }).click();
  await expect(page.locator("blockquote").first()).toContainText("300W", {
    timeout: 90000,
  });
  await page.getByRole("link", { name: "智能营养食堂", exact: true }).click();
  await page.getByRole("button", { name: "两菜一汤", exact: true }).click();
  await page.getByRole("button", { name: "生成膳食方案", exact: true }).click();
  await expect(page.locator(".meal-card")).toHaveCount(3, { timeout: 150000 });
  for (const card of await page.locator(".meal-card").all()) {
    await expect(card.locator(".meal-plan-dishes li")).toHaveCount(4);
    await expect(card.locator(".meal-plan-dishes")).toContainText("米饭");
  }
  await expect(page.getByRole("alert")).toHaveCount(0);
  await page.screenshot({
    path: "test-results/ui-real-canteen.png",
    fullPage: true,
  });
  expect(errors).toEqual([]);
});
