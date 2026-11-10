<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import PageHeading from "../../../shared/components/PageHeading.vue";
import { schemas } from "../model/resource-schemas.js";
import ResourceManager from "../components/ResourceManager.vue";
import StudentManager from "../components/StudentManager.vue";
import LeaveApproval from "../components/LeaveApproval.vue";
import BusinessRecords from "../components/BusinessRecords.vue";
const route = useRoute();
const resource = computed(() => route.params.resource);
const meta = computed(
  () =>
    schemas[resource.value] ||
    {
      students: {
        title: "学生管理",
        description: "维护学生资料、班级与登录密码。",
      },
      "leave-requests": {
        title: "请假审批",
        description: "核对申请信息，审批结果会同步到学生个人中心。",
      },
      "borrow-records": {
        title: "借阅记录",
        description: "实时查看借阅与归还情况，历史记录保留。",
      },
      orders: {
        title: "订单记录",
        description: "查看学生已确认的套餐与下单快照。",
      },
    }[resource.value],
);
</script>
<template>
  <template v-if="meta"
    ><PageHeading
      :title="meta.title"
      :description="meta.description"
      eyebrow="CAMPUS ADMINISTRATION" /><ResourceManager
      v-if="schemas[resource]"
      :key="resource"
      :resource="resource" /><StudentManager
      v-else-if="resource === 'students'" /><LeaveApproval
      v-else-if="resource === 'leave-requests'" /><BusinessRecords
      v-else
      :key="resource"
      :resource="resource"
  /></template>
  <section v-else class="empty-state">管理页面不存在</section>
</template>
