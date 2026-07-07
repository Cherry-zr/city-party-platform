<template>
  <div class="admin-card">
    <el-alert title="第一阶段预留举报管理入口，完整处理流程将在第二阶段实现。" type="info" show-icon />
    <el-table :data="rows" border style="margin-top:16px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="targetType" label="对象类型" />
      <el-table-column prop="targetId" label="对象ID" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="createdAt" label="创建时间" min-width="160" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminReports } from '../../api/admin'

const rows = ref([])

onMounted(async () => {
  const data = await adminReports({ current: 1, size: 20 })
  rows.value = data.records
})
</script>
