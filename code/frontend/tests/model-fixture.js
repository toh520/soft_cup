import http from "node:http";
// 仅供浏览器集成测试的协议夹具。业务和数据库仍由 Java 后端实际处理。
http
  .createServer(async (request, response) => {
    try {
      let raw = "";
      for await (const chunk of request) raw += chunk;
      const body = JSON.parse(raw);
      response.setHeader("Content-Type", "application/json");
      if (request.url === "/embeddings") {
        const value = body.input.includes("无关") ? [0, 1] : [1, 0];
        response.end(JSON.stringify({ data: [{ embedding: value }] }));
        return;
      }
      const system = body.messages[0].content;
      const input = JSON.parse(body.messages[1].content);
      let answer;
      if (system.includes("本餐用餐要求解析")) {
        const requirement = input.requirements;
        const rice = input.menu.find((dish) => dish.name === "米饭");
        answer = {
          stapleCount: 1,
          sideCount: 2,
          soupCount: /不要汤/.test(requirement)
            ? 0
            : /汤/.test(requirement)
              ? 1
              : 0,
          meatCount: /全素/.test(requirement)
            ? 0
            : /荤/.test(requirement)
              ? 1
              : -1,
          stapleIds: /米饭/.test(requirement) && rice ? [rice.id] : [],
          requiredDishIds: [],
          excludedDishIds: [],
          vegetarianOnly: /全素/.test(requirement),
          veganOnly: false,
          requiredAnyGroups: [],
          excludedIngredients: [],
          nutritionLimits: {},
          message: /牛排/.test(requirement) ? "今日没有牛排，请调整要求" : "",
        };
        if (requirement.includes("850"))
          answer.nutritionLimits = { energyKcal: { min: null, max: 850 } };
        if (requirement.includes("只要")) {
          const selected = input.menu.filter((dish) =>
            requirement.includes(dish.name),
          );
          answer.requiredDishIds = selected.map((dish) => dish.id);
          answer.stapleCount = selected.filter(
            (dish) => dish.category === "staple",
          ).length;
          answer.soupCount = selected.filter(
            (dish) => dish.category === "soup",
          ).length;
          answer.sideCount =
            selected.length - answer.stapleCount - answer.soupCount;
          answer.meatCount = -1;
        }
      } else if (system.includes("单餐套餐推荐"))
        answer = {
          plans: input.candidates
            .slice(0, input.planCount ?? 3)
            .map((candidate) => ({
              candidateId: candidate.candidateId,
              reason: "来自真实菜品候选的均衡组合（测试模型）",
            })),
        };
      else if (system.includes("校园资料问答")) {
        const entry = input.entries[0];
        answer = entry
          ? {
              answerable: true,
              answer: entry.content,
              citations: [{ id: entry.id, quote: entry.content }],
            }
          : { answerable: false };
      } else if (input.message.includes("图书"))
        answer = { intent: "books", query: "Java", reply: "请选定图书" };
      else if (input.message.includes("课表"))
        answer = {
          intent: "schedule",
          weekday: 1,
          date: null,
          reply: "查看课表",
        };
      else if (input.message.includes("宿舍"))
        answer = { intent: "knowledge", reply: "请到知识问答" };
      else if (input.message.includes("补充")) {
        const start = new Date(Date.now() + 3 * 86400000);
        start.setUTCHours(1, 0, 0, 0);
        const end = new Date(start.getTime() + 3600000);
        answer = {
          intent: "leave",
          startAt: start.toISOString(),
          endAt: end.toISOString(),
          reason: "测试事务",
          reply: "请核对申请表",
        };
      } else
        answer = {
          intent: "leave",
          startAt: null,
          endAt: null,
          reason: null,
          reply: "请补充具体起止时间和理由。",
        };
      response.end(
        JSON.stringify({
          choices: [{ message: { content: JSON.stringify(answer) } }],
        }),
      );
    } catch {
      response.statusCode = 500;
      response.end(JSON.stringify({ error: "fixture failure" }));
    }
  })
  .listen(8091, "127.0.0.1", () =>
    console.log("测试模型协议夹具：http://127.0.0.1:8091"),
  );
