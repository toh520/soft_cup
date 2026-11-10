<script setup>
import { computed, ref } from "vue";
import DishArtwork from "./DishArtwork.vue";
import AppIcon from "../../../shared/components/AppIcon.vue";
const props = defineProps({ dishes: Array, busy: Boolean, cart: Object });
defineEmits(["add", "refresh"]);
const category = ref("all"),
  query = ref("");
const categories = [
  ["all", "全部菜品"],
  ["staple", "主食"],
  ["protein", "蛋白类"],
  ["vegetable", "蔬菜"],
];
const filtered = computed(() =>
  props.dishes.filter(
    (dish) =>
      (category.value === "all" || dish.category === category.value) &&
      (!query.value ||
        dish.name.includes(query.value) ||
        dish.ingredientTags.some((tag) => tag.includes(query.value))),
  ),
);
const quantity = (id) =>
  props.cart?.plan?.dishes.find((dish) => dish.id === id)?.quantity ||
  (props.cart?.plan?.dishes.some((dish) => dish.id === id) ? 1 : 0);
</script>
<template>
  <section class="menu-section" aria-labelledby="menu-title">
    <div class="menu-heading">
      <div>
        <h2 id="menu-title">今日菜单</h2>
        <p>也可以自己选，把喜欢的装进这一餐。</p>
      </div>
      <button class="text-button" :disabled="busy" @click="$emit('refresh')">
        刷新菜单
      </button>
    </div>
    <div class="menu-tools">
      <div class="category-tabs" aria-label="菜品分类">
        <button
          v-for="[value, label] in categories"
          :key="value"
          :class="{ selected: category === value }"
          :aria-pressed="category === value"
          @click="category = value"
        >
          {{ label }}
        </button>
      </div>
      <label class="menu-search"
        ><AppIcon name="search" :size="17" /><input
          v-model="query"
          aria-label="搜索菜品"
          placeholder="搜索菜名、食材"
      /></label>
    </div>
    <div v-if="!filtered.length" class="empty-state">
      没有找到菜品，试试其他分类或关键词。
    </div>
    <div class="dish-grid">
      <article
        v-for="dish in filtered"
        :key="dish.id"
        class="dish-card"
        :data-dish-id="dish.id"
      >
        <div :class="['dish-picture', dish.category]">
          <DishArtwork :category="dish.category" :variant="dish.id" /><span
            v-if="dish.isVegetarian"
            class="vegetarian-tag"
            ><AppIcon name="leaf" :size="12" />素食</span
          ><span v-if="quantity(dish.id)" class="dish-selected"
            >已选 {{ quantity(dish.id) }} 份</span
          >
        </div>
        <div class="dish-card-body">
          <h3>{{ dish.name }}</h3>
          <p class="dish-portion">
            {{ dish.portionG }} g / 份 <span>{{ dish.energyKcal }} kcal</span>
          </p>
          <p class="dish-ingredients" :title="dish.ingredientTags.join('、')">
            食材：{{
              dish.ingredientTags.length
                ? dish.ingredientTags.join("、")
                : "未标注"
            }}
          </p>
          <div class="dish-card-footer">
            <span>蛋白质 {{ dish.proteinG }}g</span
            ><button
              class="add-dish"
              :disabled="busy || quantity(dish.id) >= 20"
              :aria-label="`加入${dish.name}`"
              @click="$emit('add', dish)"
            >
              <span aria-hidden="true">＋</span>
            </button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
