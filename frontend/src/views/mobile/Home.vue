<template>
  <van-nav-bar title="同城活动" right-text="后台" @click-right="goAdmin" />
  <div class="mobile-content">
    <van-search v-model="query.keyword" placeholder="搜索活动、地点、说明" @search="load" />
    <van-tabs v-model:active="query.category" sticky @change="load">
      <van-tab title="全部" name="" />
      <van-tab v-for="item in categories" :key="item" :title="item" :name="item" />
    </van-tabs>
    <section v-if="auth.isLogin" class="recommendation-section">
      <div class="recommendation-heading">
        <div class="section-title">为你推荐</div>
        <van-button
          size="small"
          type="primary"
          plain
          :loading="locating"
          loading-text="定位中"
          @click="useLocationOptimization"
        >
          使用位置优化
        </van-button>
      </div>
      <van-skeleton v-if="recommendationLoading && recommendations.length === 0" title :row="4" />
      <div v-else-if="recommendationError && recommendations.length === 0" class="recommendation-state">
        <span>推荐暂不可用</span>
        <van-button size="small" plain @click="loadRecommendations()">重试</van-button>
      </div>
      <van-empty v-else-if="recommendations.length === 0" description="暂无推荐活动" />
      <div v-for="item in recommendations" :key="item.activity.id" class="recommendation-item">
        <ActivityCard :activity="item.activity" @click="openActivity(item.activity.id)" />
        <div class="recommendation-meta">
          <van-tag type="success" plain>推荐指数 {{ item.recommendationScore }}</van-tag>
          <div class="recommendation-reasons">
            <span v-for="reason in item.reasons || []" :key="reason">{{ reason }}</span>
          </div>
        </div>
      </div>
    </section>
    <div class="section-title">活动发现</div>
    <van-empty v-if="!loading && activities.length === 0" description="暂无活动" />
    <ActivityCard v-for="item in activities" :key="item.id" :activity="item" @click="openActivity(item.id)" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import ActivityCard from '../../components/ActivityCard.vue'
import { listActivities } from '../../api/activity'
import { listRecommendedActivities } from '../../api/recommendation'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const activities = ref([])
const recommendationLoading = ref(false)
const recommendationError = ref(false)
const recommendations = ref([])
const recommendationLocation = ref(null)
const locating = ref(false)
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

async function loadRecommendations(location = recommendationLocation.value) {
  recommendationLoading.value = true
  recommendationError.value = false
  try {
    const params = { limit: 6 }
    if (location) {
      params.longitude = location.longitude
      params.latitude = location.latitude
    }
    const data = await listRecommendedActivities(params)
    recommendations.value = Array.isArray(data) ? data : []
  } catch {
    recommendationError.value = true
  } finally {
    recommendationLoading.value = false
  }
}

function useLocationOptimization() {
  if (!navigator.geolocation) {
    showFailToast('当前浏览器不支持定位，已保留当前推荐')
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    async ({ coords }) => {
      const location = {
        longitude: coords.longitude,
        latitude: coords.latitude
      }
      recommendationLocation.value = location
      try {
        await loadRecommendations(location)
      } finally {
        locating.value = false
      }
    },
    () => {
      locating.value = false
      showFailToast('未能获取位置，已保留当前推荐')
    },
    { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
  )
}

function openActivity(id) {
  router.push(`/activities/${id}`)
}

function goAdmin() {
  router.push(auth.isAdmin ? '/admin/dashboard' : '/profile')
}

onMounted(() => {
  load()
  if (auth.isLogin) {
    loadRecommendations()
  }
})
</script>

<style scoped>
.recommendation-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.recommendation-heading .section-title {
  margin-right: auto;
}

.recommendation-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72px;
  padding: 12px 14px;
  color: #5f6b7a;
  background: #fff;
  border: 1px solid #eceff3;
  border-radius: 8px;
}

.recommendation-item {
  margin-bottom: 12px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.recommendation-item :deep(.activity-card) {
  margin-bottom: 0;
  border: 0;
  border-radius: 0;
}

.recommendation-meta {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px 12px;
  border-top: 1px solid #f0f1f3;
}

.recommendation-reasons {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}
</style>
