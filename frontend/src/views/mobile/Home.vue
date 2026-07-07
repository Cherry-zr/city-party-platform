<template>
  <van-nav-bar title="同城活动" right-text="后台" @click-right="goAdmin" />
  <div class="mobile-content">
    <van-search v-model="query.keyword" placeholder="搜索活动、地点、说明" @search="load" />
    <van-tabs v-model:active="query.category" sticky @change="load">
      <van-tab title="全部" name="" />
      <van-tab v-for="item in categories" :key="item" :title="item" :name="item" />
    </van-tabs>
    <div class="section-title">活动发现</div>
    <van-empty v-if="!loading && activities.length === 0" description="暂无活动" />
    <ActivityCard v-for="item in activities" :key="item.id" :activity="item" @click="$router.push(`/activities/${item.id}`)" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import ActivityCard from '../../components/ActivityCard.vue'
import { listActivities } from '../../api/activity'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const activities = ref([])
const categories = ['观影', '聚餐', '运动', '桌游', '学习', '探店', '户外', '游戏', '展览', '其他']
const query = reactive({ keyword: '', category: '' })

async function load() {
  loading.value = true
  try {
    const data = await listActivities({ ...query, current: 1, size: 20 })
    activities.value = data.records
  } finally {
    loading.value = false
  }
}

function goAdmin() {
  router.push(auth.isAdmin ? '/admin/dashboard' : '/profile')
}

onMounted(load)
</script>
