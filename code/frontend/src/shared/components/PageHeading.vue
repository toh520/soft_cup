<script setup>
defineProps({
  eyebrow: { default: "CAMPUS SERVICES" },
  title: String,
  description: String,
});
</script>
<template>
  <div class="page-heading">
    <div>
      <h1>{{ title }}</h1>
      <p class="muted">{{ description }}</p>
    </div>
    <slot />
  </div>
</template>
