<template>
  <div class="admin-card">
    <el-table :data="rows" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="activity.title" label="活动" min-width="180" />
      <el-table-column prop="user.nickname" label="报名用户" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="applyMessage" label="申请说明" />
      <el-table-column prop="createdAt" label="报名时间" min-width="160" />
    </el-table>
    <el-pagination layout="total, prev, pager, next" :total="total" :page-size="query.size" @current-change="page => { query.current = page; load() }" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminSignups } from '../../api/admin'

const query = reactive({ current: 1, size: 10 })
const rows = ref([])
const total = ref(0)

async function load() {
  const data = await adminSignups(query)
  rows.value = data.records
  total.value = data.total
}

onMounted(load)
</script>
