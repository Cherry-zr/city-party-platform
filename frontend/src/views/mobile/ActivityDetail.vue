<template>
  <van-nav-bar title="活动详情" left-arrow @click-left="$router.back()" />
  <div v-if="activity" class="mobile-content">
    <img class="activity-cover" :src="activity.coverUrl || fallback" alt="cover" />
    <div class="plain-panel">
      <h2>{{ activity.title }}</h2>
      <div class="tag-row">
        <van-tag type="primary">{{ activity.category }}</van-tag>
        <van-tag v-for="tag in activity.tags" :key="tag" plain>{{ tag }}</van-tag>
      </div>
      <p>{{ activity.description }}</p>
      <van-cell title="时间" :value="`${formatDateTime(activity.startTime)} - ${formatDateTime(activity.endTime)}`" />
      <van-cell title="地点" :label="activity.address" :value="activity.city" />
      <van-cell title="人数" :value="`${activity.approvedCount}/${activity.maxParticipants}`" />
      <van-cell title="费用" :value="formatCost(activity)" :label="activity.aaRule" />
      <van-cell title="报名状态" :value="activity.signupStatus || '未报名'" />
    </div>
    <div class="plain-panel" @click="$router.push(`/users/${activity.creator.id}`)">
      <van-space>
        <van-image round width="44" height="44" :src="activity.creator.avatarUrl || avatarFallback" />
        <div>
          <strong>{{ activity.creator.nickname }}</strong>
          <div class="activity-meta">{{ activity.creator.city }} · 信用分 {{ activity.creator.creditScore }}</div>
        </div>
      </van-space>
    </div>
    <van-space fill>
      <van-button block type="primary" :disabled="activity.signupStatus === 'APPROVED' || activity.signupStatus === 'PENDING'" @click="doSignup">
        {{ signupText }}
      </van-button>
      <van-button block plain type="danger" :disabled="!activity.signupStatus || activity.signupStatus === 'CANCELLED'" @click="doCancel">退出</van-button>
      <van-button block plain :type="activity.favorited ? 'danger' : 'default'" @click="toggleFavorite">
        {{ activity.favorited ? '取消收藏' : '收藏' }}
      </van-button>
    </van-space>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showSuccessToast } from 'vant'
import { getActivity } from '../../api/activity'
import { signupActivity, cancelSignup } from '../../api/signup'
import { favoriteActivity, unfavoriteActivity } from '../../api/favorite'
import { formatCost, formatDateTime } from '../../utils/format'

const route = useRoute()
const activity = ref(null)
const fallback = 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=900&q=80'
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'

const signupText = computed(() => {
  if (!activity.value) return '报名'
  if (activity.value.signupStatus === 'PENDING') return '待审核'
  if (activity.value.signupStatus === 'APPROVED') return '已报名'
  return activity.value.needApproval ? '申请报名' : '立即报名'
})

async function load() {
  activity.value = await getActivity(route.params.id)
}

async function doSignup() {
  await signupActivity(activity.value.id, { applyMessage: '希望参加这个活动' })
  showSuccessToast('报名已提交')
  await load()
}

async function doCancel() {
  await cancelSignup(activity.value.id)
  showSuccessToast('已退出')
  await load()
}

async function toggleFavorite() {
  if (activity.value.favorited) {
    await unfavoriteActivity(activity.value.id)
  } else {
    await favoriteActivity(activity.value.id)
  }
  showSuccessToast('操作成功')
  await load()
}

onMounted(load)
</script>
