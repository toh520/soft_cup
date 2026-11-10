<script setup>
import { ref, onMounted } from "vue";
import { adminApi } from "../api/admin-api.js";
import { schemas, labels } from "../model/resource-schemas.js";
import { demoKnowledge } from "../model/demo-knowledge.js";
import ResourceEditor from "./ResourceEditor.vue";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
const props = defineProps({ resource: String });
const schema = schemas[props.resource];
const items = ref([]),
  editing = ref(false),
  selected = ref(null),
  busy = ref(false),
  error = ref(""),
  message = ref(""),
  query = ref(""),
  category = ref(""),
  example = ref("");
async function load() {
  busy.value = true;
  error.value = "";
  try {
    const rows = await adminApi.list(props.resource);
    items.value = rows;
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
async function save(body) {
  busy.value = true;
  error.value = "";
  message.value = "";
  try {
    if (selected.value?.id)
      await adminApi.update(props.resource, selected.value.id, body);
    else await adminApi.create(props.resource, body);
    editing.value = false;
    message.value = "数据已保存";
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function remove(item) {
  if (!confirm("确认删除此记录？存在关联业务历史时会被拒绝。")) return;
  busy.value = true;
  error.value = "";
  try {
    await adminApi.remove(props.resource, item);
    message.value = "已删除";
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function rebuild() {
  if (
    !confirm("确认重建全部知识向量？这会调用 embedding 服务；全部成功才切换。")
  )
    return;
  busy.value = true;
  error.value = "";
  try {
    await adminApi.rebuild();
    message.value = "向量已全部重建";
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
function edit(item = null) {
  selected.value = item;
  editing.value = false;
  setTimeout(() => {
    editing.value = true;
  }, 0);
}
function display(value) {
  return typeof value === "boolean"
    ? value
      ? "是"
      : "否"
    : labels[value] || value;
}
function filtered() {
  return items.value.filter(
    (item) =>
      (!category.value || item.category === category.value) &&
      (!query.value ||
        JSON.stringify(item).toLowerCase().includes(query.value.toLowerCase())),
  );
}
</script>
<template>
  <FeedbackMessage :loading="busy" :error="error" :message="message" />
  <section class="panel">
    <div class="toolbar">
      <input
        v-model="query"
        aria-label="搜索记录"
        placeholder="搜索当前列表…"
      /><select
        v-if="resource === 'knowledge-entries'"
        v-model="category"
        aria-label="知识分类"
      >
        <option value="">全部分类</option>
        <option
          v-for="key in ['rule', 'course', 'activity', 'other']"
          :key="key"
          :value="key"
        >
          {{ labels[key] }}
        </option></select
      ><button class="primary" :disabled="busy" @click="edit()">+ 新增</button
      ><button :disabled="busy" @click="load">刷新</button
      ><button
        v-if="resource === 'knowledge-entries'"
        :disabled="busy"
        @click="rebuild"
      >
        重建全部向量
      </button>
    </div>
    <div v-if="resource === 'knowledge-entries'" class="demo-picker">
      <select v-model="example" aria-label="选择演示资料">
        <option value="">选择一条虚构演示资料…</option>
        <option
          v-for="(item, index) in demoKnowledge"
          :key="index"
          :value="index"
        >
          {{ item.title }}
        </option></select
      ><button
        :disabled="example === '' || busy"
        @click="edit(demoKnowledge[example])"
      >
        填入表单</button
      ><small>保存后才调用模型生成向量。</small>
    </div>
    <div v-if="!filtered().length" class="empty-state">没有符合条件的记录</div>
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th v-for="[, label] in schema.columns" :key="label">
              {{ label }}
            </th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filtered()" :key="item.id">
            <td v-for="[key] in schema.columns" :key="key">
              {{ display(item[key]) }}
            </td>
            <td class="table-actions">
              <button :disabled="busy" @click="edit(item)">编辑</button
              ><button
                class="danger-text"
                :disabled="busy"
                @click="remove(item)"
              >
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
  <ResourceEditor
    v-if="editing"
    :key="selected?.id || example || 'new'"
    :schema="schema"
    :item="selected"
    :busy="busy"
    @save="save"
    @cancel="editing = false"
  />
</template>
