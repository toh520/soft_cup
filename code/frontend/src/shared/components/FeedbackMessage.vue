<script setup>
defineProps({ error: String, message: String, loading: Boolean });
</script>
<template>
  <p v-if="loading" class="notice" role="status">
    <span class="loading-indicator" aria-hidden="true"></span>正在处理，请稍候…
  </p>
  <p v-if="error" class="notice error" role="alert">{{ error }}</p>
  <p v-if="message" class="notice success" role="status">{{ message }}</p>
</template>
