<script setup>
import { computed } from "vue";
import AppIcon from "../../../shared/components/AppIcon.vue";
const props = defineProps({
  cart: Object,
  busy: Boolean,
  error: String,
  message: String,
});
defineEmits(["quantity", "clear", "checkout"]);
const items = computed(() =>
  props.cart?.empty ? [] : props.cart?.plan?.dishes || [],
);
const portions = computed(() =>
  items.value.reduce((count, dish) => count + (dish.quantity || 1), 0),
);
</script>
<template>
  <aside id="canteen-cart" class="canteen-cart" aria-labelledby="cart-title">
    <div class="cart-heading">
      <div class="cart-title-icon"><AppIcon name="cart" /></div>
      <h2 id="cart-title">本餐购物车</h2>
      <span class="cart-count">{{ portions }}</span>
    </div>
    <div class="cart-source">
      <span v-if="items.length">{{
        cart.plan.source === "manual" ? "手动搭配" : "AI 推荐套餐"
      }}</span
      ><span v-else>从一道喜欢的菜开始</span
      ><button
        v-if="items.length"
        class="text-button"
        :disabled="busy"
        @click="$emit('clear')"
      >
        清空
      </button>
    </div>
    <div v-if="!items.length" class="cart-empty">
      <div class="empty-plate"><AppIcon name="nutrition" :size="28" /></div>
      <h3>这一餐，想吃点什么？</h3>
      <p>选用 AI 套餐，或从菜单自由加菜。</p>
    </div>
    <ul v-else class="cart-items">
      <li v-for="dish in items" :key="dish.id">
        <div class="cart-item-title">
          <strong>{{ dish.name }}</strong
          ><button
            class="remove-dish"
            :disabled="busy"
            :aria-label="`移除${dish.name}`"
            @click="$emit('quantity', dish.id, 0)"
          >
            ×
          </button>
        </div>
        <div class="cart-item-detail">
          <small>{{ dish.energyKcal }} kcal / 份</small>
          <div class="quantity-control">
            <button
              :disabled="busy"
              :aria-label="`减少${dish.name}`"
              @click="$emit('quantity', dish.id, (dish.quantity || 1) - 1)"
            >
              −</button
            ><span :aria-label="`${dish.name}份数`">{{
              dish.quantity || 1
            }}</span
            ><button
              :disabled="busy || (dish.quantity || 1) >= 20"
              :aria-label="`增加${dish.name}`"
              @click="$emit('quantity', dish.id, (dish.quantity || 1) + 1)"
            >
              ＋
            </button>
          </div>
        </div>
      </li>
    </ul>
    <div v-if="items.length" class="cart-nutrition">
      <div class="cart-energy">
        <span>本餐合计</span
        ><strong>{{ cart.plan.totals.energyKcal }}<small> kcal</small></strong>
      </div>
      <div class="cart-macros">
        <span
          >蛋白质 <b>{{ cart.plan.totals.proteinG }}g</b></span
        ><span
          >脂肪 <b>{{ cart.plan.totals.fatG }}g</b></span
        ><span
          >碳水 <b>{{ cart.plan.totals.carbohydrateG }}g</b></span
        >
      </div>
    </div>
    <p
      v-if="cart && !cart.empty && !cart.valid"
      class="notice error"
      role="alert"
    >
      {{ cart.error }}
    </p>
    <p v-if="error" class="notice error" role="alert">{{ error }}</p>
    <p v-if="message" class="notice success" role="status">{{ message }}</p>
    <div class="cart-bottom">
      <button
        class="primary wide checkout-button"
        :disabled="busy || !items.length || !cart.valid"
        @click="$emit('checkout')"
      >
        {{ busy ? "正在处理…" : "提交订单" }}<AppIcon name="arrow" :size="18" />
      </button>
      <p>确认后记录订单，无需支付。</p>
    </div>
  </aside>
</template>
