<script setup>
import { useCanteen } from "../composables/useCanteen.js";
import { useAuth } from "../../auth/composables/useAuth.js";
import PageHeading from "../../../shared/components/PageHeading.vue";
import AppIcon from "../../../shared/components/AppIcon.vue";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
import DishArtwork from "../components/DishArtwork.vue";
import MealPlanCard from "../components/MealPlanCard.vue";
import DishMenu from "../components/DishMenu.vue";
import CanteenCart from "../components/CanteenCart.vue";
const { user } = useAuth();
const {
  readiness,
  dishes,
  plans,
  target,
  cart,
  loading,
  recommending,
  cartBusy,
  error,
  cartError,
  message,
  portions,
  load,
  recommend,
  select,
  quantity,
  add,
  clear,
  checkout,
} = useCanteen();
function showCart() {
  document.getElementById("canteen-cart")?.scrollIntoView({
    behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches
      ? "instant"
      : "smooth",
    block: "start",
  });
}
</script>
<template>
  <div class="canteen-page">
    <PageHeading
      title="智能营养食堂"
      description="一餐一份好心情。让 AI 帮你搭配，也可以自己挑喜欢的。"
      ><button class="cart-jump" @click="showCart">
        <AppIcon name="cart" :size="18" />本餐购物车<span>{{ portions }}</span>
      </button></PageHeading
    >
    <section class="ai-dining-hero" aria-labelledby="ai-title">
      <div class="ai-hero-copy">
        <span class="ai-feature-label"
          ><AppIcon name="sparkle" :size="15" />为你搭配这一餐</span
        >
        <h2 id="ai-title">今天吃什么，<br />交给 AI 想一想。</h2>
        <p>
          结合身体信息与饮食偏好，<br
            class="desktop-break"
          />从今日菜品里挑出三个均衡方案。
        </p>
        <div class="ai-hero-actions">
          <button
            class="ai-generate"
            :disabled="loading || recommending || !readiness?.ready"
            @click="recommend"
          >
            <AppIcon name="sparkle" :size="18" />{{
              recommending ? "正在搭配这一餐…" : "生成三个膳食方案"
            }}<AppIcon v-if="!recommending" name="arrow" :size="18" /></button
          ><span v-if="target">本餐目标约 {{ target }} kcal</span
          ><span v-else-if="readiness?.ready"
            >{{ user?.name }}，你的身体资料已就绪</span
          >
        </div>
      </div>
      <div class="ai-hero-art">
        <DishArtwork hero /><span class="plate-caption"
          >一份主食 · 一份蛋白 · 一份蔬菜</span
        >
      </div>
    </section>
    <div v-if="readiness && !readiness.ready" class="profile-nudge">
      <AppIcon name="profile" />
      <p>
        <strong>补全身体信息，让推荐更适合你</strong
        ><span
          >尚需补全：{{
            readiness.missing.join("、")
          }}。手动点餐可直接使用。</span
        >
      </p>
      <RouterLink class="button" to="/profile">去补全资料</RouterLink>
    </div>
    <FeedbackMessage :loading="loading" :error="error" />
    <div v-if="recommending" class="recommendation-wait" role="status">
      <span class="waiting-orbit"></span>
      <div>
        <strong>正在从今日菜品中为你搭配</strong>
        <p>通常需要十几秒，等待时也可以继续浏览和手动点餐。</p>
      </div>
    </div>
    <section v-if="plans.length" class="recommendation-section">
      <div class="recommendation-heading">
        <div>
          <span class="section-kicker"
            ><AppIcon name="sparkle" :size="16" />为你推荐</span
          >
          <h2>三种搭配，挑一餐喜欢的。</h2>
        </div>
        <p>选用套餐将直接替换购物车中的菜品。</p>
      </div>
      <div class="meal-grid">
        <MealPlanCard
          v-for="(plan, index) in plans"
          :key="plan.id"
          :plan="plan"
          :index="index + 1"
          :busy="cartBusy"
          @select="select"
        />
      </div>
    </section>
    <div class="canteen-layout">
      <DishMenu
        :dishes="dishes"
        :cart="cart"
        :busy="cartBusy || loading"
        @add="add"
        @refresh="load"
      /><CanteenCart
        :cart="cart"
        :busy="cartBusy"
        :error="cartError"
        :message="message"
        @quantity="quantity"
        @clear="clear"
        @checkout="checkout"
      />
    </div>
    <p class="canteen-footnote">
      <AppIcon
        name="leaf"
        :size="14"
      />营养数据为估算值，非医疗建议。严重过敏请向食堂核实，食材标签不能保证无交叉接触。
    </p>
  </div>
</template>
