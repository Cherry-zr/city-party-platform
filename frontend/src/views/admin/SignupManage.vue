<template>
  <div class="admin-card">
    <el-form inline>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 180px">
          <el-option label="待审核" value="PENDING" />
          <el-option label="报名成功" value="APPROVED" />
          <el-option label="候补中" value="WAITING" />
          <el-option label="候补转正" value="PROMOTED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="load">筛选</el-button>
    </el-form>
    <el-table :data="rows" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="activity.title" label="活动" min-width="180" />
      <el-table-column prop="user.nickname" label="报名用户" />
      <el-table-column label="状态">
        <template #default="{ row }">{{ statusText(row.status) }}</template>
      </el-table-column>
      <el-table-column prop="applyMessage" label="申请说明" />
      <el-table-column prop="createdAt" label="报名时间" min-width="160" />
    </el-table>
    <el-pagination layout="total, prev, pager, next" :total="total" :page-size="query.size" @current-change="page => { query.current = page; load() }" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminSignups } from '../../api/admin'

const query = reactive({ status: '', current: 1, size: 10 })
const rows = ref([])
const total = ref(0)

async function load() {
  const data = await adminSignups(query)
  rows.value = data.records
  total.value = data.total
}

function statusText(status) {
  const map = {
    PENDING: '待审核',
    APPROVED: '报名成功',
    WAITING: '候补中',
    PROMOTED: '候补转正',
    CANCELLED: '已取消',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

onMounted(load)
</script>
