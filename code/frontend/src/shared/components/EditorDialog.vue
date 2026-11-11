<script setup>
import { onMounted, onBeforeUnmount, ref, useId } from "vue";
const props = defineProps({ title: String, busy: Boolean });
const emit = defineEmits(["close"]);
const dialog = ref(null);
const headingId = useId();
let previousFocus;
let previousOverflow;
onMounted(() => {
  previousFocus = document.activeElement;
  previousOverflow = document.body.style.overflow;
  document.body.style.overflow = "hidden";
  dialog.value.showModal();
  dialog.value.querySelector("input, select, textarea")?.focus();
});
onBeforeUnmount(() => {
  dialog.value?.close();
  document.body.style.overflow = previousOverflow;
  previousFocus?.focus();
});
// 保存期间不关闭表单，避免用户误以为写入被取消；原生 dialog 提供焦点约束。
function close() {
  if (!props.busy) emit("close");
}
</script>
<template>
  <Teleport to="body">
    <dialog
      ref="dialog"
      class="editor-dialog"
      :aria-labelledby="headingId"
      @cancel.prevent="close"
    >
      <header class="dialog-heading">
        <div>
          <p>校园管理</p>
          <h2 :id="headingId">{{ title }}</h2>
        </div>
        <button
          type="button"
          class="dialog-close"
          aria-label="关闭编辑窗口"
          :disabled="busy"
          @click="close"
        >
          ×
        </button>
      </header>
      <div class="dialog-content"><slot /></div>
    </dialog>
  </Teleport>
</template>
