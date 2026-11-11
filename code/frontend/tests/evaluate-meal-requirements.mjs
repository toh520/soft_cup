import assert from "node:assert/strict";
import { writeFile, mkdir } from "node:fs/promises";
import { additionalDishes, evaluationCases } from "./meal-evaluation-cases.js";

// 显式启用隔离服务写入；独立规则逐一核查所有套餐，记录每次失败，重试不覆盖首轮结果。
const base = process.env.CAMPUS_REAL_TEST_URL;
assert(
  base && process.env.CAMPUS_EVALUATION_ISOLATED === "1",
  "必须指定隔离服务并设置 CAMPUS_EVALUATION_ISOLATED=1",
);
const selected = process.env.CAMPUS_EVALUATION_CASES?.split(",");
const cases = evaluationCases.filter(
  (item) => !selected || selected.includes(item.id),
);
const report = [];
async function api(path, body, token, method = "POST") {
  const response = await fetch(`${base}/api${path}`, {
    method,
    signal: AbortSignal.timeout(300000),
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(method === "GET" ? {} : { body: JSON.stringify(body ?? {}) }),
  });
  return { status: response.status, data: await response.json() };
}
const admin = (
  await api("/auth/login", { account: "admin", password: "123456" })
).data.token;
assert(admin);
let menu = (await api("/admin/dishes", null, admin, "GET")).data;
for (const [
  name,
  category,
  isVegetarian,
  energyKcal,
  proteinG,
  fatG,
  carbohydrateG,
  ingredientTags,
] of additionalDishes) {
  if (menu.some((dish) => dish.name === name)) continue;
  const added = await api(
    "/admin/dishes",
    {
      name,
      category,
      isVegetarian,
      energyKcal,
      proteinG,
      fatG,
      carbohydrateG,
      ingredientTags,
      portionG: 250,
      isAvailable: true,
    },
    admin,
  );
  assert.equal(added.status, 200);
}
menu = (await api("/admin/dishes", null, admin, "GET")).data;
const available = new Map(
  menu.filter((dish) => dish.isAvailable).map((dish) => [dish.id, dish]),
);
function verify(item, result) {
  if (item.reject || (item.allowReject && result.status === 400)) {
    assert.equal(result.status, 400);
    return;
  }
  assert.equal(result.status, 200, result.data.message);
  const plans = result.data.plans;
  assert.equal(plans.length, item.exactPlanCount ?? 3, "方案数量");
  const unique = new Set();
  for (const plan of plans) {
    const dishes = plan.dishes;
    for (const dish of dishes) {
      assert(available.has(dish.id), "无供应/虚构ID");
      assert.deepEqual(dish, available.get(dish.id), "菜单快照不一致");
    }
    unique.add(
      dishes
        .map((dish) => dish.id)
        .sort((a, b) => a - b)
        .join(","),
    );
    assert.equal(
      new Set(dishes.map((dish) => dish.id)).size,
      dishes.length,
      "重复菜品",
    );
    const sides = dishes.filter((dish) =>
      ["protein", "vegetable"].includes(dish.category),
    );
    if (item.counts)
      assert.deepEqual(
        [
          dishes.filter((d) => d.category === "staple").length,
          sides.length,
          dishes.filter((d) => d.category === "soup").length,
        ],
        item.counts,
        "主食/配菜/汤数量",
      );
    if (item.meat !== undefined)
      assert.equal(
        sides.filter((dish) => !dish.isVegetarian).length,
        item.meat,
        "荤素数量",
      );
    if (item.vegetarian)
      assert(
        dishes.every((dish) => dish.isVegetarian),
        "素食包含荤菜",
      );
    if (item.staple)
      assert(
        dishes
          .filter((dish) => dish.category === "staple")
          .every((dish) => item.staple.includes(dish.name)),
        "主食不匹配",
      );
    for (const name of item.required ?? [])
      assert(
        dishes.some((dish) => dish.name === name),
        `缺少${name}`,
      );
    for (const group of item.any ?? [])
      assert(
        dishes.some((dish) => group.includes(dish.name)),
        "任选菜品不符合",
      );
    for (const ingredient of item.avoid ?? [])
      assert(
        dishes.every(
          (dish) =>
            !dish.ingredientTags.some((tag) => tag.includes(ingredient)),
        ),
        `含禁止食材${ingredient}`,
      );
    for (const nutrient of ["energyKcal", "proteinG", "fatG", "carbohydrateG"])
      assert.equal(
        plan.totals[nutrient],
        Math.round(dishes.reduce((sum, dish) => sum + dish[nutrient], 0) * 10) /
          10,
        "营养合计错误",
      );
    if (item.maxFat) assert(plan.totals.fatG <= item.maxFat, "脂肪超出要求");
    if (item.maxCarbohydrate)
      assert(plan.totals.carbohydrateG <= item.maxCarbohydrate, "碳水超出要求");
    if (item.onlyMeatIngredient)
      assert(
        sides
          .filter((dish) => !dish.isVegetarian)
          .every((dish) =>
            dish.ingredientTags.includes(item.onlyMeatIngredient),
          ),
        "荤菜食材不符合限定",
      );
    if (item.maxEnergy)
      assert(plan.totals.energyKcal <= item.maxEnergy, "超过要求热量");
    if (item.minEnergy)
      assert(plan.totals.energyKcal >= item.minEnergy, "低于要求热量");
    if (item.minProtein)
      assert(plan.totals.proteinG >= item.minProtein, "蛋白质不足");
  }
  assert.equal(unique.size, plans.length, "重复套餐");
}
async function run(item) {
  const token = (
    await api("/auth/login", {
      account: item.vegetarianProfile ? "20260002" : "20260001",
      password: "123456",
    })
  ).data.token;
  const started = Date.now();
  let result;
  let failure;
  try {
    result = await api(
      "/nutrition/recommendations",
      { requirements: item.text },
      token,
    );
    verify(item, result);
  } catch (error) {
    failure = error.message;
  }
  const entry = {
    id: item.id,
    text: item.text,
    passed: !failure,
    status: result?.status,
    seconds: Math.round((Date.now() - started) / 100) / 10,
    failure,
    message: result?.data?.message,
    dishes: result?.data?.plans?.map((plan) =>
      plan.dishes.map((dish) => dish.name),
    ),
    totals: result?.data?.plans?.map((plan) => plan.totals),
  };
  report.push(entry);
  console.log(JSON.stringify(entry));
  await api("/auth/logout", {}, token);
}
let next = 0;
await Promise.all(
  Array.from({ length: 2 }, async () => {
    while (next < cases.length) await run(cases[next++]);
  }),
);
await mkdir("test-results", { recursive: true });
const filename = `test-results/meal-evaluation-${Date.now()}.json`;
await writeFile(
  filename,
  JSON.stringify(
    {
      menuCount: available.size,
      passed: report.filter((r) => r.passed).length,
      total: report.length,
      results: report,
    },
    null,
    2,
  ),
);
console.log(
  `Report: ${filename}; ${report.filter((r) => r.passed).length}/${report.length} passed`,
);
process.exitCode = report.every((entry) => entry.passed) ? 0 : 1;
