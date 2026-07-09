<template>
  <van-nav-bar title="我的评价" left-arrow @click-left="$router.back()" />
  <van-tabs v-model:active="activeType" sticky offset-top="46" @change="load">
    <van-tab title="我发出的" name="sent" />
    <van-tab title="我收到的" name="received" />
  </van-tabs>
  <div class="mobile-content">
    <div v-if="loading" class="page-state">
      <van-loading vertical>正在加载评价...</van-loading>
    </div>
    <van-empty v-else-if="errorMessage" image="error" :description="errorMessage">
      <van-button size="small" type="primary" plain @click="load">重新加载</van-button>
    </van-empty>
    <van-empty v-else-if="reviews.length === 0" description="暂无评价" />
    <template v-else>
      <div v-for="review in reviews" :key="review.id" class="plain-panel">
        <div class="activity-title">{{ review.activityTitle }}</div>
        <van-space align="center" fill>
          <van-image
            round
            width="42"
            height="42"
            :src="counterpartAvatar(review) || avatarFallback"
          />
          <div class="review-main">
            <div>{{ counterpartName(review) }}</div>
            <van-rate :model-value="review.rating" readonly size="16" />
          </div>
          <van-tag :type="review.creditDelta >= 0 ? 'success' : 'danger'">
            {{ formatDelta(review.creditDelta) }}
          </van-tag>
        </van-space>
        <p v-if="review.content" class="review-content">{{ review.content }}</p>
        <div v-if="review.tags?.length" class="tag-row">
          <van-tag v-for="tag in review.tags" :key="tag" plain>{{ tag }}</van-tag>
        </div>
        <div class="review-footer">
          <div class="activity-meta">{{ formatDateTime(review.createdAt) }}</div>
          <van-button size="mini" plain @click="$router.push(`/activities/${review.activityId}`)">查看活动</van-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyReviews } from '../../api/review'
import { formatDateTime } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const activeType = ref(route.query.type === 'received' ? 'received' : 'sent')
const reviews = ref([])
const loading = ref(false)
const errorMessage = ref('')
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getMyReviews({ type: activeType.value, current: 1, size: 50 })
    reviews.value = data.records || []
    router.replace({ path: '/reviews/my', query: { type: activeType.value } })
  } catch (error) {
    reviews.value = []
    errorMessage.value = error.message || '评价加载失败'
  } finally {
    loading.value = false
  }
}

function counterpartName(review) {
  return activeType.value === 'sent'
    ? `评价给 ${review.targetNickname}`
    : `来自 ${review.reviewerNickname}`
}

function counterpartAvatar(review) {
  return activeType.value === 'sent' ? review.targetAvatarUrl : review.reviewerAvatarUrl
}

function formatDelta(value) {
  if (!value) return '信用分 0'
  return `信用分 ${value > 0 ? '+' : ''}${value}`
}

onMounted(load)
</script>

<style scoped>
.review-main {
  flex: 1;
  min-width: 0;
  color: #303943;
  font-size: 14px;
}

.review-content {
  margin: 12px 0 6px;
  color: #303943;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.review-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}
</style>
