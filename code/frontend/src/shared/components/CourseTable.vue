<script setup>
defineProps({ courses: { default: () => [] } });
const days = ["", "周一", "周二", "周三", "周四", "周五", "周六", "周日"];
</script>
<template>
  <div v-if="!courses.length" class="empty-state">暂无课程安排</div>
  <div v-else class="table-scroll">
    <table>
      <thead>
        <tr>
          <th>课程</th>
          <th>星期</th>
          <th>时间</th>
          <th>教师</th>
          <th>教室</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="course in courses" :key="course.id">
          <td>{{ course.courseName }}</td>
          <td>{{ days[course.weekday] }}</td>
          <td>{{ course.startTime }}–{{ course.endTime }}</td>
          <td>{{ course.teacher }}</td>
          <td>{{ course.classroom }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
