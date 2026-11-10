<script setup>
defineProps({ name: String, size: { default: 20 } });
const paths = {
  assistant:
    "M7 4h10a4 4 0 0 1 4 4v6a4 4 0 0 1-4 4h-5l-5 4v-4a4 4 0 0 1-4-4V8a4 4 0 0 1 4-4Z M8 10h8 M8 14h5",
  knowledge:
    "M12 5c-3-2-6-2-9-1v15c3-1 6-1 9 1 3-2 6-2 9-1V4c-3-1-6-1-9 1Zm0 0v15",
  nutrition: "M4 3v6a3 3 0 0 0 6 0V3 M7 3v18 M17 3v18 M17 3c5 2 5 9 0 9",
  profile: "M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2",
  cart: "M3 4h2l2 12h12l2-9H6 M9 21h.01 M18 21h.01",
  sparkle: "m12 2 2.7 7.3L22 12l-7.3 2.7L12 22l-2.7-7.3L2 12l7.3-2.7L12 2Z",
  search: "M16 10a6 6 0 1 1-12 0 6 6 0 0 1 12 0Zm-1.5 4.5L21 21",
  calendar: "M4 5h16v16H4V5Zm0 5h16 M8 2v6 M16 2v6",
  check: "m5 12 4 4 10-10",
  arrow: "M5 12h14m-5-5 5 5-5 5",
  leaf: "M20 3C5 1 1 12 7 17S23 15 20 3ZM4 21 16 8",
  logout: "M9 4H4v16h5 M9 12h12m-5-5 5 5-5 5",
  settings: "M4 6h16 M4 12h16 M4 18h16 M8 3v6 M16 9v6 M10 15v6",
};
</script>
<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.6"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path :d="paths[name] || paths.settings" />
  </svg>
</template>
