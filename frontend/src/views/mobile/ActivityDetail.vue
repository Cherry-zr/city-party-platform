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
      <van-cell title="地图位置" :label="mapLocationText">
        <template #right-icon>
          <van-button v-if="activity.longitude && activity.latitude" size="small" plain type="primary" @click.stop="viewMap">查看地图</van-button>
        </template>
      </van-cell>
      <van-cell title="人数" :value="`${activity.approvedCount}/${activity.maxParticipants}`" />
      <van-cell title="候补人数" :value="`${activity.waitlistCount || 0}`" />
      <van-cell title="费用" :value="formatCost(activity)" :label="activity.aaRule" />
      <van-cell title="报名状态" :value="statusText(activity.signupStatus)" />
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
    <div class="plain-panel">
      <van-button block :type="chatAccess.canAccess ? 'primary' : 'default'" plain @click="openChat">
        {{ chatButtonText }}
      </van-button>
      <div v-if="!chatAccess.canAccess" class="activity-meta chat-access-reason">{{ chatAccess.reason }}</div>
    </div>
    <van-space fill>
      <van-button block type="primary" :disabled="primaryDisabled" @click="doSignup">
        {{ signupText }}
      </van-button>
      <van-button block plain type="danger" :disabled="!activity.signupStatus || activity.signupStatus === 'CANCELLED'" @click="doCancel">{{ cancelText }}</van-button>
      <van-button block plain :type="activity.favorited ? 'danger' : 'default'" @click="toggleFavorite">
        {{ activity.favorited ? '取消收藏' : '收藏' }}
      </van-button>
    </van-space>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { cancelWaitlist, getActivity, joinWaitlist } from '../../api/activity'
import { checkChatAccess } from '../../api/chat'
import { signupActivity, cancelSignup } from '../../api/signup'
import { favoriteActivity, unfavoriteActivity } from '../../api/favorite'
import { useAuthStore } from '../../stores/auth'
import { formatCost, formatDateTime } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const activity = ref(null)
const chatAccess = ref({
  canAccess: false,
  reason: '请先登录后进入活动群聊',
  activityId: null,
  activityTitle: ''
})
const fallback = 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=900&q=80'
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'

const signupText = computed(() => {
  if (!activity.value) return '报名'
  if (activity.value.signupStatus === 'PENDING') return '待审核'
  if (activity.value.signupStatus === 'APPROVED') return '已报名'
  if (activity.value.signupStatus === 'WAITING') return '候补中'
  if (activity.value.canJoinWaitlist) return '加入候补'
  return activity.value.needApproval ? '申请报名' : '立即报名'
})

const primaryDisabled = computed(() => ['APPROVED', 'PENDING', 'WAITING'].includes(activity.value?.signupStatus))
const cancelText = computed(() => activity.value?.signupStatus === 'WAITING' ? '取消候补' : '退出')
const chatButtonText = computed(() => chatAccess.value.canAccess ? '进入活动群聊' : '活动群聊暂不可进入')

const mapLocationText = computed(() => {
  if (!activity.value?.longitude || !activity.value?.latitude) return '暂无经纬度'
  return `${activity.value.longitude}, ${activity.value.latitude}`
})

async function load() {
  activity.value = await getActivity(route.params.id)
  await loadChatAccess()
}

async function loadChatAccess() {
  if (!auth.isLogin) {
    chatAccess.value = {
      canAccess: false,
      reason: '请先登录后进入活动群聊',
      activityId: activity.value?.id,
      activityTitle: activity.value?.title || ''
    }
    return
  }
  try {
    chatAccess.value = await checkChatAccess(activity.value.id)
  } catch (error) {
    chatAccess.value = {
      canAccess: false,
      reason: error.message || '暂时无法检查群聊权限',
      activityId: activity.value?.id,
      activityTitle: activity.value?.title || ''
    }
  }
}

async function doSignup() {
  if (activity.value.canJoinWaitlist) {
    await joinWaitlist(activity.value.id)
    showSuccessToast('已加入候补')
  } else {
    await signupActivity(activity.value.id, { applyMessage: '希望参加这个活动' })
    showSuccessToast('报名已提交')
  }
  await load()
}

async function doCancel() {
  if (activity.value.signupStatus === 'WAITING') {
    await cancelWaitlist(activity.value.id)
    showSuccessToast('已取消候补')
  } else {
    await cancelSignup(activity.value.id)
    showSuccessToast('已退出')
  }
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

function openChat() {
  if (!chatAccess.value.canAccess) {
    showToast(chatAccess.value.reason || '报名成功后才能进入活动群聊')
    return
  }
  router.push(`/activities/${activity.value.id}/chat`)
}

function viewMap() {
  router.push({
    path: '/map',
    query: {
      activityId: activity.value.id,
      longitude: activity.value.longitude,
      latitude: activity.value.latitude,
      distanceKm: 3
    }
  })
}

function statusText(status) {
  const map = {
    APPROVED: '报名成功',
    PENDING: '待审核',
    WAITING: '候补中',
    PROMOTED: '报名成功',
    CANCELLED: '已退出',
    REJECTED: '已拒绝'
  }
  return map[status] || '未报名'
}

onMounted(load)
</script>
