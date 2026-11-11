<script setup>
import { reactive } from "vue";
import EditorDialog from "../../../shared/components/EditorDialog.vue";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
const props = defineProps({
  schema: Object,
  item: Object,
  busy: Boolean,
  error: String,
});
const emit = defineEmits(["save", "cancel"]);
const form = reactive({
  ...props.schema.defaults,
  ...Object.fromEntries(
    Object.keys(props.schema.defaults).map((key) => [
      key,
      props.item?.[key] ?? props.schema.defaults[key],
    ]),
  ),
});
for (const field of props.schema.fields)
  if (field.type === "list")
    form[field.key] = (form[field.key] || []).join("、");
function submit() {
  const body = { ...form };
  for (const field of props.schema.fields)
    if (field.type === "list")
      body[field.key] = body[field.key]
        .split(/[,，、\n]/)
        .map((text) => text.trim())
        .filter(Boolean);
  emit("save", body);
}
</script>
<template>
  <EditorDialog
    :title="`${item?.id ? '编辑' : '新增'}${schema.title}`"
    :busy="busy"
    @close="emit('cancel')"
  >
    <FeedbackMessage :error="error" />
    <form @submit.prevent="submit">
      <div class="form-grid">
        <label
          v-for="field in schema.fields"
          :key="field.key"
          :class="{
            'span-two': field.type === 'textarea',
            'checkbox-label': field.type === 'checkbox',
          }"
          >{{ field.label
          }}<textarea
            v-if="field.type === 'textarea'"
            v-model="form[field.key]"
            required
            :maxlength="field.maxLength || 2000"
            rows="5"
          ></textarea
          ><select
            v-else-if="field.type === 'select'"
            v-model="form[field.key]"
            required
          >
            <option
              v-for="[value, label] in field.options"
              :key="value"
              :value="value"
            >
              {{ label }}
            </option></select
          ><input
            v-else-if="field.type === 'checkbox'"
            v-model="form[field.key]"
            type="checkbox" /><input
            v-else-if="field.type === 'number'"
            v-model.number="form[field.key]"
            type="number"
            required
            :min="field.min"
            :step="field.step || 0.1" /><input
            v-else
            v-model="form[field.key]"
            :type="field.type === 'list' ? 'text' : field.type"
            :required="field.type !== 'list'"
            :maxlength="field.type === 'list' ? 1000 : 150"
        /></label>
      </div>
      <div class="actions">
        <button class="primary" :disabled="busy">
          {{ busy ? "正在保存…" : "保存" }}</button
        ><button type="button" :disabled="busy" @click="emit('cancel')">
          取消
        </button>
      </div>
    </form>
  </EditorDialog>
</template>
