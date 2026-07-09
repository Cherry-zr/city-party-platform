<template>
  <div>
    <div class="admin-page-heading">
      <div>
        <h1>报名管理</h1>
        <p>查看报名记录，不提供踢人、删除或候补队列调整操作</p>
      </div>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="活动 ID">
          <el-input-number v-model="query.activityId" :min="1" :controls="false" placeholder="全部活动" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 180px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="报名成功" value="APPROVED" />
            <el-option label="候补中" value="WAITING" />
            <el-option label="候补转正" value="PROMOTED" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="缺席" value="ABSENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">筛选</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="rows" border empty-text="暂无报名记录">
        <el-table-column prop="id" label="ID" width="76" />
        <el-table-column prop="activityId" label="活动 ID" width="90" />
        <el-table-column prop="activity.title" label="活动" min-width="180" show-overflow-tooltip />
        <el-table-column prop="user.id" label="用户 ID" width="90" />
        <el-table-column prop="user.nickname" label="报名用户" min-width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">{{ statusText(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="applyMessage" label="申请说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="报名时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.current"
        class="admin-pagination"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminSignups } from '../../api/admin'
import { formatDateTime } from '../../utils/format'

const query = reactive({ activityId: undefined, status: '', current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await adminSignups(query)
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  Object.assign(query, { activityId: undefined, status: '', current: 1 })
  load()
}

function statusText(status) {
  const map = {
    PENDING: '待审核',
    APPROVED: '报名成功',
    WAITING: '候补中',
    PROMOTED: '候补转正',
    CANCELLED: '已取消',
    REJECTED: '已拒绝',
    COMPLETED: '已完成',
    ABSENT: '缺席'
  }
  return map[status] || status
}

onMounted(load)
</script>
