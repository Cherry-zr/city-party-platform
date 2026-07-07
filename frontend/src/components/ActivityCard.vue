<template>
  <div class="activity-card" @click="$emit('click')">
    <img class="activity-cover" :src="activity.coverUrl || fallback" alt="activity cover" />
    <div class="activity-body">
      <div class="activity-title">{{ activity.title }}</div>
      <div class="tag-row">
        <van-tag type="primary" plain>{{ activity.category }}</van-tag>
        <van-tag v-for="tag in activity.tags || []" :key="tag" plain>{{ tag }}</van-tag>
      </div>
      <div class="activity-meta">
        <div>{{ formatDateTime(activity.startTime) }} · {{ activity.city }}</div>
        <div>{{ activity.address }}</div>
        <div>{{ activity.approvedCount }}/{{ activity.maxParticipants }} 人 · {{ formatCost(activity) }}</div>
      </div>
      <van-space style="margin-top: 10px">
        <van-image round width="28" height="28" :src="activity.creator?.avatarUrl || avatarFallback" />
        <span>{{ activity.creator?.nickname || '发起人' }}</span>
        <van-tag v-if="activity.favorited" type="danger" plain>已收藏</van-tag>
      </van-space>
    </div>
  </div>
</template>

<script setup>
import { formatCost, formatDateTime } from '../utils/format'

defineProps({
  activity: {
    type: Object,
    required: true
  }
})

const fallback = 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=900&q=80'
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'
</script>
