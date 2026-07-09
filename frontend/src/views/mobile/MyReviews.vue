<template>
  <van-nav-bar title="我的评价" left-arrow @click-left="$router.back()" />
  <van-tabs v-model:active="activeType" sticky offset-top="46" @change="load">
    <van-tab title="我发出的" name="sent" />
    <van-tab title="我收到的" name="received" />
  </van-tabs>
  <div class="mobile-content">
    <van-empty v-if="!loading && reviews.length === 0" description="暂无评价" />
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
      <div class="activity-meta">{{ formatDateTime(review.createdAt) }}</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMyReviews } from '../../api/review'
import { formatDateTime } from '../../utils/format'

const activeType = ref('sent')
const reviews = ref([])
const loading = ref(false)
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'

async function load() {
  loading.value = true
  try {
    const data = await getMyReviews({ type: activeType.value, current: 1, size: 50 })
    reviews.value = data.records || []
  } catch {
    reviews.value = []
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
</style>
