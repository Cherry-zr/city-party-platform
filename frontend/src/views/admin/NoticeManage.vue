<template>
  <div>
    <div class="admin-page-heading">
      <div>
        <h1>通知管理</h1>
        <p>查看系统已产生的通知，不提供群发、删除或批量标记操作</p>
      </div>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="接收用户 ID">
          <el-input-number v-model="query.userId" :min="1" :controls="false" placeholder="全部用户" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">筛选</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="rows" border empty-text="暂无通知记录">
        <el-table-column prop="id" label="ID" width="76" />
        <el-table-column prop="userId" label="用户 ID" width="90" />
        <el-table-column label="接收用户" min-width="140">
          <template #default="{ row }">{{ row.nickname || row.username }}（{{ row.username }}）</template>
        </el-table-column>
        <el-table-column prop="type" label="类型" min-width="140" />
        <el-table-column prop="title" label="标题" min-width="170" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.read ? 'info' : 'warning'">{{ row.read ? '已读' : '未读' }}</el-tag>
          </template>
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
import { adminNotices } from '../../api/admin'
import { formatDateTime } from '../../utils/format'

const query = reactive({ userId: undefined, current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await adminNotices(query)
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
  Object.assign(query, { userId: undefined, current: 1 })
  load()
}

onMounted(load)
</script>
