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
      <el-table-column prop="longitude" label="经度" width="120" />
      <el-table-column prop="latitude" label="纬度" width="120" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="approvedCount" label="报名数" />
      <el-table-column prop="waitlistCount" label="候补数" />
      <el-table-column prop="favoriteCount" label="收藏数" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="detail = row">详情</el-button>
          <el-button size="small" type="primary" plain @click="showWaitlist(row)">候补</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination layout="total, prev, pager, next" :total="total" :page-size="query.size" @current-change="page => { query.current = page; load() }" />
    <el-drawer v-model="drawer" title="活动详情" :before-close="() => detail = null">
      <pre>{{ detail }}</pre>
    </el-drawer>
    <el-drawer v-model="waitlistDrawer" title="候补列表" size="520px">
      <el-table :data="waitlistRows" border>
        <el-table-column prop="queueNo" label="队号" width="80" />
        <el-table-column prop="user.nickname" label="用户" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="createdAt" label="加入时间" min-width="160" />
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminActivities } from '../../api/admin'
import { activityWaitlist } from '../../api/activity'

const query = reactive({ keyword: '', category: '', status: '', current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const detail = ref(null)
const waitlistDrawer = ref(false)
const waitlistRows = ref([])
const drawer = computed({ get: () => Boolean(detail.value), set: (v) => { if (!v) detail.value = null } })

async function load() {
  const data = await adminActivities(query)
  rows.value = data.records
  total.value = data.total
}

async function showWaitlist(row) {
  const data = await activityWaitlist(row.id, { current: 1, size: 50 })
  waitlistRows.value = data.records || []
  waitlistDrawer.value = true
}

onMounted(load)
</script>
