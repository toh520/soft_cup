<script setup>
import { computed } from "vue";
const props = defineProps({
  category: String,
  variant: { default: 0 },
  hero: Boolean,
});
const kind = computed(() => (props.hero ? "meal" : props.category));
const greens = ["#70a37d", "#63966f", "#8cae61", "#4f886f"];
const color = computed(() => greens[Number(props.variant) % greens.length]);
</script>
<template>
  <svg
    class="dish-artwork"
    viewBox="0 0 240 180"
    fill="none"
    aria-hidden="true"
  >
    <ellipse cx="120" cy="149" rx="77" ry="12" fill="#203149" opacity=".06" />
    <circle cx="120" cy="88" r="75" fill="#fff" />
    <circle
      cx="120"
      cy="88"
      r="64"
      fill="#f3f3eb"
      stroke="#e6e9e1"
      stroke-width="2"
    />
    <g v-if="kind === 'staple'">
      <ellipse
        cx="120"
        cy="86"
        rx="48"
        ry="43"
        :fill="variant % 2 ? '#e6cd95' : '#f6e6c7'"
      />
      <g
        v-for="n in 24"
        :key="n"
        :transform="`translate(${85 + ((n * 23) % 72)} ${55 + ((n * 17) % 61)}) rotate(${n * 37})`"
      >
        <rect width="10" height="4" rx="2" fill="#fff9e9" />
      </g>
      <path d="M109 54q14-17 26 0-14 12-26 0Z" fill="#769a73" />
      <path d="m122 44 1 20" stroke="#446d4b" stroke-width="2" />
    </g>
    <g v-else-if="kind === 'protein'">
      <g
        v-for="n in 7"
        :key="n"
        :transform="`translate(${79 + ((n * 29) % 68)} ${48 + ((n * 19) % 66)}) rotate(${n * 17})`"
      >
        <rect
          width="29"
          height="24"
          rx="7"
          :fill="variant % 2 ? '#e8b96d' : '#c87b52'"
        />
        <path
          d="m6 7 17 3M6 14l14 3"
          stroke="#ae6940"
          stroke-width="2"
          opacity=".4"
        />
      </g>
      <ellipse
        cx="99"
        cy="60"
        rx="14"
        ry="6"
        fill="#74a17b"
        transform="rotate(-25 99 60)"
      />
      <ellipse
        cx="144"
        cy="113"
        rx="17"
        ry="6"
        fill="#74a17b"
        transform="rotate(25 144 113)"
      />
    </g>
    <g v-else-if="kind === 'vegetable'">
      <g
        v-for="n in 8"
        :key="n"
        :transform="`translate(${88 + ((n * 23) % 65)} ${54 + ((n * 31) % 65)}) rotate(${n * 39})`"
      >
        <path d="M0 0c-28-15-25 26 0 29C25 18 26-13 0 0Z" :fill="color" />
        <path
          d="M0 2v25m0-10-9-8m9 11 10-10"
          stroke="#e1ecd1"
          stroke-width="1.5"
        />
      </g>
      <circle cx="147" cy="65" r="8" fill="#e7b36b" />
      <circle cx="98" cy="112" r="6" fill="#e7b36b" />
    </g>
    <g v-else>
      <path d="M122 31a57 57 0 0 0-48 85l48-29V31Z" fill="#f2dfb3" />
      <g
        v-for="n in 15"
        :key="n"
        :transform="`translate(${83 + ((n * 13) % 32)} ${46 + ((n * 17) % 50)}) rotate(${n * 29})`"
      >
        <rect width="9" height="4" rx="2" fill="#fffcf4" />
      </g>
      <g
        v-for="n in 5"
        :key="n"
        :transform="`translate(${140 + ((n * 17) % 24)} ${55 + ((n * 21) % 45)}) rotate(${n * 31})`"
      >
        <path d="M0 0c-25-15-25 21 0 26C23 14 24-12 0 0Z" fill="#80a66c" />
        <path d="M0 2v22" stroke="#cfe2b5" />
      </g>
      <g transform="translate(96 112) rotate(-15)">
        <rect width="31" height="22" rx="6" fill="#c98152" />
        <rect x="32" y="1" width="29" height="24" rx="6" fill="#d8935a" />
        <path
          d="M5 7h20M5 14h18m14-6h17m-17 7h17"
          stroke="#ac673d"
          stroke-width="2"
        />
      </g>
      <circle cx="144" cy="107" r="10" fill="#de8f71" />
    </g>
    <path
      v-if="hero"
      d="m210 35-16 114m22-114-14 113"
      stroke="#b6c4e6"
      stroke-width="3"
      stroke-linecap="round"
    />
  </svg>
</template>
