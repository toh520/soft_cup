<script setup>
import AppIcon from "../../../shared/components/AppIcon.vue";
defineProps({ plan: Object, index: Number, busy: Boolean });
defineEmits(["select"]);
const labels = { staple: "主食", protein: "蛋白", vegetable: "蔬菜" };
</script>
<template>
  <article class="meal-card">
    <div class="meal-plan-heading">
      <span class="plan-mark"
        ><AppIcon name="sparkle" :size="15" />搭配{{
          ["", "一", "二", "三"][index]
        }}</span
      ><span class="meal-plan-energy"
        ><b>{{ plan.totals.energyKcal }}</b> kcal</span
      >
    </div>
    <h3>
      {{
        plan.dishes
          .map((dish) => dish.name)
          .slice(0, 2)
          .join(" + ")
      }}
    </h3>
    <p class="meal-reason">{{ plan.reason }}</p>
    <ul class="meal-plan-dishes">
      <li v-for="dish in plan.dishes" :key="dish.id">
        <span>{{ labels[dish.category] }}</span
        ><strong>{{ dish.name }}</strong
        ><small>{{ dish.portionG }}g</small>
      </li>
    </ul>
    <div class="meal-plan-macros">
      <span>蛋白 {{ plan.totals.proteinG }}g</span
      ><span>脂肪 {{ plan.totals.fatG }}g</span
      ><span>碳水 {{ plan.totals.carbohydrateG }}g</span>
    </div>
    <button
      class="select-plan wide"
      :disabled="busy"
      @click="$emit('select', plan)"
    >
      选用套餐<AppIcon name="arrow" :size="16" />
    </button>
  </article>
</template>
