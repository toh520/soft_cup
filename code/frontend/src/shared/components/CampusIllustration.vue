<template>
  <svg
    class="campus-illustration"
    viewBox="0 0 620 320"
    fill="none"
    aria-hidden="true"
  >
    <circle cx="484" cy="67" r="32" fill="#F2D06B" />
    <path d="M27 271H593M68 289H553" stroke="#91ABD9" stroke-width="2" />
    <path
      d="M122 256V124H286V256M316 256V150H482V256"
      fill="#35599D"
      stroke="#C4D6F6"
      stroke-width="2"
    />
    <path d="M107 124L204 66L301 124H107Z" fill="#CFE5D8" />
    <path
      d="M329 150V114H468V150"
      fill="#617FB7"
      stroke="#C4D6F6"
      stroke-width="2"
    />
    <path
      d="M192 256V206Q204 188 216 206V256"
      fill="#19376F"
      stroke="#C4D6F6"
      stroke-width="2"
    />
    <g fill="#F0D78B">
      <path
        v-for="n in 5"
        :key="`left-${n}`"
        :d="`M${133 + n * 24} 145h10v18h-10z`"
      />
      <path
        v-for="n in 5"
        :key="`right-${n}`"
        :d="`M${327 + n * 24} 176h10v18h-10z`"
      />
    </g>
    <path
      d="M145 186h19v17h-19zM244 186h19v17h-19zM348 215h18v20h-18zM431 215h18v20h-18z"
      fill="#9DB5D9"
    />
    <path
      d="M80 262V188M525 262V193"
      stroke="#CFE5D8"
      stroke-width="5"
      stroke-linecap="round"
    />
    <path
      d="M80 155C42 174 41 216 80 227C119 216 118 174 80 155ZM525 160C486 179 487 219 525 231C564 219 563 179 525 160Z"
      fill="#91BBAA"
    />
    <path d="M237 272L277 302H351L388 272" stroke="#91ABD9" stroke-width="2" />
    <path
      d="M338 69q10-10 20 0q10-10 20 0M397 48q8-8 16 0q8-8 16 0"
      stroke="#C4D6F6"
      stroke-width="2"
      stroke-linecap="round"
    />
  </svg>
</template>
