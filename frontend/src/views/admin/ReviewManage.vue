<template>
  <div>
    <div class="admin-page-heading">
      <div>
        <h1>评价管理</h1>
        <p>按活动或相关用户查看评价记录</p>
      </div>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="活动 ID">
          <el-input-number v-model="query.activityId" :min="1" :controls="false" placeholder="全部活动" />
        </el-form-item>
        <el-form-item label="用户 ID">
          <el-input-number v-model="query.userId" :min="1" :controls="false" placeholder="评价人或被评价人" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">筛选</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="rows" border empty-text="暂无评价记录">
        <el-table-column prop="id" label="ID" width="76" />
        <el-table-column prop="activityId" label="活动 ID" width="90" />
        <el-table-column prop="activityTitle" label="活动" min-width="170" show-overflow-tooltip />
        <el-table-column label="评价人" min-width="130">
          <template #default="{ row }">{{ row.reviewerNickname }}（{{ row.reviewerId }}）</template>
        </el-table-column>
        <el-table-column label="被评价人" min-width="130">
          <template #default="{ row }">{{ row.targetNickname }}（{{ row.targetUserId }}）</template>
        </el-table-column>
        <el-table-column label="评分" width="150">
          <template #default="{ row }"><el-rate :model-value="row.rating" disabled /></template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="信用变化" width="90">
          <template #default="{ row }">{{ formatDelta(row.creditDelta) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="150">
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
import { adminReviews } from '../../api/admin'
import { formatDateTime } from '../../utils/format'

const query = reactive({ activityId: undefined, userId: undefined, current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await adminReviews(query)
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
  Object.assign(query, { activityId: undefined, userId: undefined, current: 1 })
  load()
}

function formatDelta(value) {
  if (!value) return '0'
  return value > 0 ? `+${value}` : value
}

onMounted(load)
</script>
