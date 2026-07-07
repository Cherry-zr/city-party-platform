<template>
  <div class="admin-card">
    <el-form inline>
      <el-form-item label="关键词"><el-input v-model="query.keyword" clearable /></el-form-item>
      <el-form-item label="分类"><el-input v-model="query.category" clearable /></el-form-item>
      <el-form-item label="状态"><el-input v-model="query.status" clearable /></el-form-item>
      <el-button type="primary" @click="load">筛选</el-button>
    </el-form>
    <el-table :data="rows" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="category" label="分类" />
      <el-table-column prop="city" label="城市" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="approvedCount" label="报名数" />
      <el-table-column prop="favoriteCount" label="收藏数" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }"><el-button size="small" @click="detail = row">详情</el-button></template>
      </el-table-column>
    </el-table>
    <el-pagination layout="total, prev, pager, next" :total="total" :page-size="query.size" @current-change="page => { query.current = page; load() }" />
    <el-drawer v-model="drawer" title="活动详情" :before-close="() => detail = null">
      <pre>{{ detail }}</pre>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminActivities } from '../../api/admin'

const query = reactive({ keyword: '', category: '', status: '', current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const detail = ref(null)
const drawer = computed({ get: () => Boolean(detail.value), set: (v) => { if (!v) detail.value = null } })

async function load() {
  const data = await adminActivities(query)
  rows.value = data.records
  total.value = data.total
}

onMounted(load)
</script>
