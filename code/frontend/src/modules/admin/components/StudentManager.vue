<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import { adminApi } from "../api/admin-api.js";
import FeedbackMessage from "../../../shared/components/FeedbackMessage.vue";
import EditorDialog from "../../../shared/components/EditorDialog.vue";
const query = ref("");
const students = ref([]),
  classes = ref([]),
  selected = ref(null),
  editing = ref(false),
  busy = ref(false),
  error = ref(""),
  message = ref(""),
  newPassword = ref("");
const form = reactive({
  name: "",
  studentNumber: "",
  password: "",
  classId: 1,
});
const filtered = computed(() =>
  students.value.filter((student) =>
    `${student.name} ${student.studentNumber} ${student.className || ""}`
      .toLowerCase()
      .includes(query.value.trim().toLowerCase()),
  ),
);
async function load() {
  busy.value = true;
  error.value = "";
  try {
    [students.value, classes.value] = await Promise.all([
      adminApi.list("students"),
      adminApi.list("classes"),
    ]);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
function edit(student = null) {
  error.value = "";
  message.value = "";
  selected.value = student;
  Object.assign(form, {
    name: student?.name || "",
    studentNumber: student?.studentNumber || "",
    password: "",
    classId: student?.classId ?? null,
  });
  newPassword.value = "";
  editing.value = true;
}
async function save() {
  busy.value = true;
  error.value = "";
  message.value = "";
  try {
    if (selected.value)
      await adminApi.update("students", selected.value.id, {
        name: form.name,
        classId: form.classId,
      });
    else
      await adminApi.create("students", {
        name: form.name,
        studentNumber: form.studentNumber,
        password: form.password,
      });
    editing.value = false;
    message.value = "学生信息已保存";
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function reset() {
  if (!confirm("确认重置此学生密码？")) return;
  busy.value = true;
  error.value = "";
  try {
    await adminApi.password(selected.value.id, newPassword.value);
    newPassword.value = "";
    message.value = "密码已重置";
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <FeedbackMessage
    v-if="!editing"
    :loading="busy"
    :error="error"
    :message="message"
  />
  <section class="panel">
    <div class="toolbar">
      <input
        v-model="query"
        aria-label="搜索学生"
        placeholder="搜索姓名、学号或班级"
      />
      <button class="primary" :disabled="busy" @click="edit()">
        + 新建学生</button
      ><button :disabled="busy" @click="load">刷新</button>
    </div>
    <div v-if="!students.length" class="empty-state">
      暂无学生，可新建账号或让学生自行注册。
    </div>
    <div v-else-if="!filtered.length" class="empty-state">
      没有符合条件的学生，请调整搜索内容。
    </div>
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>学号</th>
            <th>姓名</th>
            <th>密码</th>
            <th>班级</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="student in filtered" :key="student.id">
            <td>{{ student.studentNumber }}</td>
            <td>{{ student.name }}</td>
            <td>{{ student.password ?? "旧账号：登录一次或重置后可查看" }}</td>
            <td>{{ student.className || "未分配" }}</td>
            <td>
              <button :disabled="busy" @click="edit(student)">
                编辑 / 重置密码
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
  <EditorDialog
    v-if="editing"
    :title="selected ? '编辑学生' : '新建学生'"
    :busy="busy"
    @close="editing = false"
  >
    <FeedbackMessage :error="error" :message="message" />
    <form @submit.prevent="save">
      <div class="form-grid">
        <label>姓名<input v-model="form.name" required maxlength="50" /></label
        ><label v-if="!selected"
          >学号<input
            v-model="form.studentNumber"
            required
            pattern="[A-Za-z0-9]{1,32}" /></label
        ><label v-if="!selected"
          >密码<input
            v-model="form.password"
            type="password"
            required
            minlength="6"
            maxlength="64" /></label
        ><label v-if="selected"
          >班级<select v-model="form.classId">
            <option :value="null">未分配班级</option>
            <option v-for="item in classes" :key="item.id" :value="item.id">
              {{ item.name }}
            </option>
          </select></label
        >
      </div>
      <div class="actions">
        <button class="primary" :disabled="busy">保存</button
        ><button type="button" :disabled="busy" @click="editing = false">
          取消
        </button>
      </div>
    </form>
    <form v-if="selected" class="password-reset" @submit.prevent="reset">
      <label
        >新密码<input
          v-model="newPassword"
          type="password"
          required
          minlength="6"
          maxlength="64" /></label
      ><button :disabled="busy">重置密码</button>
    </form>
  </EditorDialog>
</template>
