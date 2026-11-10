import { test, expect } from "@playwright/test";

test("演示账号可登录，管理员可查看原密码及待向量化资料", async ({ page }) => {
  const login = await page.request.post("/api/auth/login", {
    data: { account: "admin", password: "123456" },
  });
  expect(login.ok()).toBeTruthy();
  await page.goto("/#/admin/students");
  const row = page.locator("tbody tr").filter({ hasText: "20260001" });
  await expect(row).toContainText("张晨");
  await expect(row).toContainText("123456");
  await page.getByRole("link", { name: "校园知识" }).click();
  await expect(
    page.locator("tbody tr").filter({ hasText: "演示规则：宿舍用电" }),
  ).toContainText("待向量化");
  await page.getByRole("button", { name: "退出", exact: true }).click();
  await page.getByLabel("学号 / 管理员账号").fill("20260002");
  await page.getByLabel("密码", { exact: true }).fill("123456");
  await page.getByRole("button", { name: "登录校园" }).click();
  await expect(
    page.getByRole("heading", { name: "校园服务助手", exact: true }),
  ).toBeVisible();
  await page.locator("nav").getByRole("link", { name: "个人中心" }).click();
  await expect(page.getByLabel("姓名", { exact: true })).toHaveValue("林悦");
  await expect(page.getByLabel("饮食偏好")).toHaveValue("vegetarian");
  const profile = await (await page.request.get("/api/profile")).json();
  expect(profile).not.toHaveProperty("password");
  const access = await page.request.get("/api/admin/students");
  expect(access.status()).toBe(403);
});
