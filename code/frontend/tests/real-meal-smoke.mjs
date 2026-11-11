import assert from "node:assert/strict";

// 手工启用的真实模型验收，必须指向隔离数据库服务，不输出凭据或模型配置。
const base = process.env.CAMPUS_REAL_TEST_URL;
assert(base, "请显式设置指向隔离服务的 CAMPUS_REAL_TEST_URL");
async function request(path, body, token, method = "POST") {
  const response = await fetch(`${base}/api${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(method === "GET" ? {} : { body: JSON.stringify(body ?? {}) }),
  });
  return { status: response.status, data: await response.json() };
}
const first = (
  await request("/auth/login", { account: "20260001", password: "123456" })
).data.token;
const vegetarian = (
  await request("/auth/login", { account: "20260002", password: "123456" })
).data.token;
assert(first && vegetarian);
const cases = [
  {
    name: "用户原句",
    text: "我希望有荤有素，2菜一汤，主食是米饭",
    token: first,
    count: 4,
    soup: 1,
    meat: 1,
    rice: true,
  },
  {
    name: "中文数量",
    text: "两菜一汤，一荤一素，主食只要米饭",
    token: first,
    count: 4,
    soup: 1,
    meat: 1,
    rice: true,
  },
  {
    name: "不要汤",
    text: "米饭配两个菜，一荤一素，不要汤",
    token: first,
    count: 3,
    soup: 0,
    meat: 1,
    rice: true,
  },
  {
    name: "素食档案",
    text: "全素，两菜一汤，主食米饭",
    token: vegetarian,
    count: 4,
    soup: 1,
    meat: 0,
    rice: true,
    vegetarian: true,
  },
  { name: "留空默认", text: "", token: first, count: 3, soup: 0 },
  { name: "无供应菜品", text: "我要牛排，两菜一汤", token: first, fail: true },
  {
    name: "档案冲突",
    text: "我要一份鸡肉，米饭，两菜一汤",
    token: vegetarian,
    fail: true,
  },
  {
    name: "数量冲突",
    text: "两道菜，但要三荤两素，再加一汤",
    token: first,
    fail: true,
  },
];
let selected;
const selectedCases = process.env.CAMPUS_REAL_CASES?.split(",");
for (const item of cases.filter(
  (item) => !selectedCases || selectedCases.includes(item.name),
)) {
  const started = Date.now();
  const result = await request(
    "/nutrition/recommendations",
    { requirements: item.text },
    item.token,
  );
  if (item.fail) {
    assert.equal(
      result.status,
      400,
      `${item.name}: ${JSON.stringify(result.data)}`,
    );
    console.log(
      JSON.stringify({
        name: item.name,
        status: result.status,
        message: result.data.message,
        seconds: (Date.now() - started) / 1000,
      }),
    );
    continue;
  }
  assert.equal(
    result.status,
    200,
    `${item.name}: ${JSON.stringify(result.data)}`,
  );
  assert.equal(result.data.plans.length, 3);
  const unique = new Set();
  for (const plan of result.data.plans) {
    assert.equal(plan.dishes.length, item.count, item.name);
    assert.equal(
      plan.dishes.filter((dish) => dish.category === "soup").length,
      item.soup,
    );
    if (item.rice)
      assert.deepEqual(
        plan.dishes
          .filter((dish) => dish.category === "staple")
          .map((dish) => dish.name),
        ["米饭"],
      );
    if (item.meat !== undefined)
      assert.equal(
        plan.dishes.filter(
          (dish) =>
            ["protein", "vegetable"].includes(dish.category) &&
            !dish.isVegetarian,
        ).length,
        item.meat,
      );
    if (item.vegetarian) assert(plan.dishes.every((dish) => dish.isVegetarian));
    unique.add(
      plan.dishes
        .map((dish) => dish.id)
        .sort((a, b) => a - b)
        .join(","),
    );
  }
  assert.equal(unique.size, 3);
  if (item.name === "用户原句") {
    const cart = await request(
      "/cart",
      { planId: result.data.plans[0].id },
      first,
      "PUT",
    );
    assert.equal(cart.status, 200);
    selected = cart.data.plan;
    const order = await request(
      "/orders",
      { token: selected.checkoutToken },
      first,
    );
    assert.equal(order.status, 200);
    const orders = await request("/orders", undefined, first, "GET");
    assert.equal(orders.data[0].items.length, 4);
  }
  console.log(
    JSON.stringify({
      name: item.name,
      status: result.status,
      dishes: result.data.plans.map((plan) =>
        plan.dishes.map((dish) => dish.name),
      ),
      seconds: (Date.now() - started) / 1000,
    }),
  );
}
console.log("本次所选真实模型场景验收通过");
