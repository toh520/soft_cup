import { test, expect } from "@playwright/test";
import { loginPage, tabApi } from "./tab-api.js";

test.skip(process.env.CAMPUS_MODEL_FIXTURE !== "1", "需要模型协议夹具");

test("米饭两菜一汤一荤一素：推荐、选用、四道菜下单、历史和手机显示", async ({
  page,
}) => {
  await loginPage(page, "20260001");
  await page.goto("/#/nutrition");
  const requirements = page.getByLabel("本次用餐要求（选填）");
  await requirements.fill("我希望有荤有素，2菜一汤，主食是米饭");
  await page.getByRole("button", { name: "加入米饭", exact: true }).click();
  await expect(page.locator("#canteen-cart .cart-items li")).toHaveCount(1);
  const previous = await (await tabApi(page).get("/api/cart")).json();
  const request = page.waitForRequest("**/api/nutrition/recommendations");
  await page.getByRole("button", { name: "生成膳食方案" }).click();
  expect((await request).postDataJSON().requirements).toContain("2菜一汤");
  await expect(page.locator(".meal-card")).toHaveCount(3);
  for (const card of await page.locator(".meal-card").all()) {
    await expect(card.locator(".meal-plan-dishes li")).toHaveCount(4);
    await expect(card.locator(".meal-plan-dishes")).toContainText("米饭");
    await expect(
      card.locator(".meal-plan-dishes li").filter({ hasText: "汤" }),
    ).toHaveCount(1);
  }
  expect((await (await tabApi(page).get("/api/cart")).json()).plan.id).toBe(
    previous.plan.id,
  );
  await page
    .getByRole("button", { name: "选用套餐", exact: true })
    .first()
    .click();
  await expect(page.locator("#canteen-cart .cart-items li")).toHaveCount(4);
  const selected = (await (await tabApi(page).get("/api/cart")).json()).plan;
  expect(
    selected.dishes.filter(
      (d) => ["protein", "vegetable"].includes(d.category) && !d.isVegetarian,
    ),
  ).toHaveLength(1);
  expect(
    (
      await tabApi(page).post("/api/orders", {
        data: { token: previous.plan.checkoutToken },
      })
    ).status(),
  ).toBe(409);
  await page
    .locator(".ai-dining-hero")
    .screenshot({ path: "test-results/meal-requirements-hero.png" });
  await page.screenshot({
    path: "test-results/meal-requirements-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page
    .locator(".ai-dining-hero")
    .screenshot({ path: "test-results/meal-requirements-mobile-hero.png" });
  await page.screenshot({
    path: "test-results/meal-requirements-mobile.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "提交订单", exact: true }).click();
  await expect(page.getByText(/订单 #\d+ 已提交/)).toBeVisible();
  const orders = await (await tabApi(page).get("/api/orders")).json();
  expect(orders[0].items).toHaveLength(4);
});

test("空白默认、全素带汤、不要汤、无供应和长度限制", async ({ page }) => {
  await loginPage(page, "20260002");
  await page.goto("/#/nutrition");
  const requirements = page.getByLabel("本次用餐要求（选填）");
  await expect(requirements).toHaveAttribute("maxlength", "300");
  const before = await (await tabApi(page).get("/api/profile")).json();
  for (const [text, length] of [
    ["   ", 3],
    ["全素，两菜一汤，主食米饭", 4],
    ["全素，米饭，不要汤", 3],
  ]) {
    await requirements.fill(text);
    await page.getByRole("button", { name: "生成膳食方案" }).click();
    await expect(page.locator(".meal-card")).toHaveCount(3);
    await expect(
      page.locator(".meal-card").first().locator(".meal-plan-dishes li"),
    ).toHaveCount(length);
    await page
      .getByRole("button", { name: "选用套餐", exact: true })
      .first()
      .click();
    await expect(page.locator("#canteen-cart .cart-items li")).toHaveCount(
      length,
    );
    const plan = (await (await tabApi(page).get("/api/cart")).json()).plan;
    expect(plan.dishes.every((dish) => dish.isVegetarian)).toBeTruthy();
  }
  const cartBefore = await (await tabApi(page).get("/api/cart")).json();
  await requirements.fill("我要牛排");
  await page.getByRole("button", { name: "生成膳食方案" }).click();
  await expect(page.getByRole("alert")).toContainText("今日没有牛排");
  await expect(requirements).toHaveValue("我要牛排");
  await expect(page.locator(".meal-card")).toHaveCount(0);
  expect((await (await tabApi(page).get("/api/cart")).json()).plan.id).toBe(
    cartBefore.plan.id,
  );
  expect(await (await tabApi(page).get("/api/profile")).json()).toEqual(before);
});

test("管理员可新增汤，学生汤分类可见，关闭供应后消失", async ({
  page,
  context,
}) => {
  await loginPage(page, "admin");
  await page.getByRole("link", { name: "食堂菜品" }).click();
  await page.getByRole("button", { name: "+ 新增", exact: true }).click();
  const name = `浏览器汤品${Date.now()}`;
  await page.getByLabel("菜品名称", { exact: true }).fill(name);
  await page.getByRole("combobox", { name: /类别/ }).selectOption("soup");
  await page.getByRole("button", { name: "保存", exact: true }).click();
  await expect(page.getByText("数据已保存")).toBeVisible();
  const student = await context.newPage();
  await loginPage(student, "20260001");
  await student.goto("/#/nutrition");
  await student.getByRole("button", { name: "汤", exact: true }).click();
  await expect(
    student.locator(".dish-card").filter({ hasText: name }),
  ).toBeVisible();
  await page
    .locator("tbody tr")
    .filter({ hasText: name })
    .getByRole("button", { name: "编辑", exact: true })
    .click();
  await page.getByLabel("是否供应", { exact: true }).uncheck();
  await page.getByRole("button", { name: "保存", exact: true }).click();
  await expect(page.getByText("数据已保存")).toBeVisible();
  await student.getByRole("button", { name: "刷新菜单", exact: true }).click();
  await expect(
    student.locator(".dish-card").filter({ hasText: name }),
  ).toHaveCount(0);
  await student.close();
});

test("精确点菜只返回一个方案，营养限制与条件摘要可核对", async ({ page }) => {
  await loginPage(page, "20260001");
  await page.goto("/#/nutrition");
  await page
    .getByLabel("本次用餐要求（选填）")
    .fill("只要米饭、清蒸鱼、清炒西兰花、紫菜豆腐汤，每样一份");
  await page.getByRole("button", { name: "生成膳食方案" }).click();
  await expect(page.locator(".meal-card")).toHaveCount(1);
  await expect(page.getByText(/符合全部条件的搭配仅有1种/)).toBeVisible();
  await expect(page.getByText(/本次搭配条件/)).toContainText("必含");
  await page.getByRole("button", { name: "选用套餐", exact: true }).click();
  await expect(page.locator("#canteen-cart .cart-items li")).toHaveCount(4);
  await page
    .getByLabel("本次用餐要求（选填）")
    .fill("米饭两菜一汤，一荤一素，不超过850千卡");
  await page.getByRole("button", { name: "生成膳食方案" }).click();
  await expect(page.locator(".meal-card")).toHaveCount(3);
  await expect(page.getByText(/本次搭配条件/)).toContainText("850");
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page
    .locator(".recommendation-section")
    .screenshot({ path: "test-results/meal-conditions-mobile.png" });
});
