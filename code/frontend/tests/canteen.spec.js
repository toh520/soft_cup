import { tabApi } from "./tab-api.js";
import { test, expect } from "@playwright/test";

async function openCanteen(page) {
  const account = `M${Date.now()}${Math.floor(Math.random() * 1000)}`;
  await tabApi(page).post("/api/auth/register", {
    data: {
      name: "食堂体验学生",
      studentNumber: account,
      password: "campus123",
    },
  });
  await tabApi(page).post("/api/auth/login", {
    data: { account, password: "campus123" },
  });
  await page.goto("/#/nutrition");
  await expect(page.locator(".dish-card")).not.toHaveCount(0);
}

test("手动点餐、搜索分类、份数、刷新恢复、食堂内下单与历史数量", async ({
  page,
}) => {
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await openCanteen(page);
  await expect(
    page.locator("nav").getByRole("link", { name: "我的购物车" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "生成膳食方案" }),
  ).toBeDisabled();
  await page.getByRole("button", { name: "主食", exact: true }).click();
  expect(await page.locator(".dish-picture:not(.staple)").count()).toBe(0);
  await page.getByLabel("搜索菜品").fill("米饭");
  await expect(page.locator(".dish-card")).toHaveCount(1);
  await page.getByRole("button", { name: "加入米饭", exact: true }).click();
  const cart = page.locator("#canteen-cart");
  await expect(cart).toContainText("手动搭配");
  await cart.getByRole("button", { name: "增加米饭", exact: true }).click();
  await expect(cart.getByLabel("米饭份数", { exact: true })).toHaveText("2");
  expect(await (await tabApi(page).get("/api/orders")).json()).toHaveLength(0);
  await page.reload();
  await expect(cart.getByLabel("米饭份数", { exact: true })).toHaveText("2");
  await page.getByLabel("搜索菜品").fill("");
  await page.getByRole("button", { name: "蔬菜", exact: true }).click();
  const vegetable = page.locator(".dish-card").first();
  const name = await vegetable.locator("h3").innerText();
  await vegetable
    .getByRole("button", { name: `加入${name}`, exact: true })
    .click();
  await cart.getByRole("button", { name: `移除${name}`, exact: true }).click();
  await cart.getByRole("button", { name: "提交订单", exact: true }).click();
  await expect(cart.getByText(/订单 #\d+ 已提交/)).toBeVisible();
  const orders = await (await tabApi(page).get("/api/orders")).json();
  expect(orders).toHaveLength(1);
  expect(orders[0].items).toHaveLength(1);
  expect(orders[0].items[0].quantity).toBe(2);
  await page.locator("nav").getByRole("link", { name: "个人中心" }).click();
  await page.getByRole("button", { name: "订单记录", exact: true }).click();
  await expect(page.getByText("米饭 ×2", { exact: true })).toBeVisible();
  await page.goto("/#/cart");
  await expect(page).toHaveURL(/nutrition/);
  expect(errors).toEqual([]);
});

test("桌面和窄屏的菜单、购物车、空状态与视觉截图", async ({ page }) => {
  await openCanteen(page);
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: "instant" }));
  await page.screenshot({
    path: "test-results/canteen-desktop.png",
    fullPage: true,
  });
  for (const width of [1024, 768, 390]) {
    await page.setViewportSize({ width, height: 900 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBeTruthy();
    await page.getByRole("button", { name: "本餐购物车" }).click();
    await expect(
      page.getByRole("heading", { name: "本餐购物车", exact: true }),
    ).toBeInViewport();
  }
  await page.getByRole("button", { name: "加入米饭", exact: true }).click();
  await page.getByRole("button", { name: "本餐购物车" }).click();
  await expect(page.locator("#canteen-cart")).toContainText("米饭");
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: "instant" }));
  await page.screenshot({
    path: "test-results/canteen-mobile.png",
    fullPage: true,
  });
  await page
    .locator("#canteen-cart")
    .getByRole("button", { name: "清空", exact: true })
    .click();
  await expect(page.getByText("这一餐，想吃点什么？")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "提交订单", exact: true }),
  ).toBeDisabled();
});

test("AI 生成不改变手动购物车，选用套餐整体覆盖后可继续调整", async ({
  page,
}) => {
  test.skip(process.env.CAMPUS_MODEL_FIXTURE !== "1", "需要模型协议夹具");
  await openCanteen(page);
  await tabApi(page).put("/api/profile", {
    data: {
      name: "食堂体验学生",
      age: 22,
      sex: "male",
      heightCm: 175,
      weightKg: 65,
      activityLevel: "medium",
      dietPreference: "none",
      avoidedIngredients: [],
    },
  });
  await page.reload();
  await page.getByRole("button", { name: "加入米饭", exact: true }).click();
  const before = await (await tabApi(page).get("/api/cart")).json();
  await page.getByRole("button", { name: "生成膳食方案" }).click();
  await expect(page.locator(".meal-card")).toHaveCount(3);
  expect((await (await tabApi(page).get("/api/cart")).json()).plan.id).toBe(
    before.plan.id,
  );
  await page
    .getByRole("button", { name: "选用套餐", exact: true })
    .first()
    .click();
  const cart = page.locator("#canteen-cart");
  await expect(cart).toContainText("AI 推荐套餐");
  await expect(cart.getByText("已选用 AI 套餐，购物车已替换。")).toBeVisible();
  await expect(cart.locator(".cart-items li")).toHaveCount(3);
  expect(
    (
      await tabApi(page).post("/api/orders", {
        data: { token: before.plan.checkoutToken },
      })
    ).status(),
  ).toBe(409);
  await cart.locator(".quantity-control button").last().click();
  await expect(cart).toContainText("手动搭配");
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: "instant" }));
  await page.screenshot({
    path: "test-results/canteen-ai-selection.png",
    fullPage: true,
  });
});
