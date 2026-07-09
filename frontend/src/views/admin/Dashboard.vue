<template>
  <div v-loading="loading">
    <div class="admin-page-heading">
      <div>
        <h1>数据看板</h1>
        <p>平台核心业务数据概览</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-row :gutter="16">
      <el-col v-for="item in cards" :key="item.label" :xs="24" :sm="12" :md="8" :lg="4">
        <div class="admin-card dashboard-card">
          <div class="dashboard-label">{{ item.label }}</div>
          <div class="dashboard-value">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { dashboard } from '../../api/admin'

const loading = ref(false)
const data = ref({ userCount: 0, activityCount: 0, signupCount: 0, reviewCount: 0, noticeCount: 0 })
const cards = computed(() => [
  { label: '用户数', value: data.value.userCount || 0 },
  { label: '活动数', value: data.value.activityCount || 0 },
  { label: '报名数', value: data.value.signupCount || 0 },
  { label: '评价数', value: data.value.reviewCount || 0 },
  { label: '通知数', value: data.value.noticeCount || 0 }
])

async function load() {
  loading.value = true
  try {
    data.value = await dashboard()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
