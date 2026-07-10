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
      <van-cell title="活动状态" :value="activityStatusText(activity.status)" />
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
    <div v-if="canReview" class="plain-panel">
      <van-button block plain type="primary" @click="router.push(`/activities/${activity.id}/reviews`)">
        评价成员
      </van-button>
      <div class="activity-meta review-entry-tip">活动已结束，可以查看并评价本次活动成员。</div>
    </div>
    <div v-if="isCreator" class="plain-panel">
      <van-space fill>
        <van-button block plain type="primary" :loading="actionLoading" @click="editActivity">编辑活动</van-button>
        <van-button block plain type="warning" :loading="actionLoading" :disabled="!canFinishActivity" @click="finishCurrentActivity">结束活动</van-button>
        <van-button block plain type="danger" :loading="actionLoading" :disabled="!canCancelActivity" @click="cancelCurrentActivity">取消活动</van-button>
      </van-space>
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
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import { cancelActivity, cancelWaitlist, finishActivity, getActivity, joinWaitlist } from '../../api/activity'
import { checkChatAccess } from '../../api/chat'
import { signupActivity, cancelSignup } from '../../api/signup'
import { favoriteActivity, unfavoriteActivity } from '../../api/favorite'
import { useAuthStore } from '../../stores/auth'
import { formatCost, formatDateTime } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const activity = ref(null)
const actionLoading = ref(false)
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
  if (isCreator.value) return '发起人无需报名'
  if (activity.value.signupStatus === 'PENDING') return '待审核'
  if (activity.value.signupStatus === 'APPROVED') return '已报名'
  if (activity.value.signupStatus === 'WAITING') return '候补中'
  if (activity.value.canJoinWaitlist) return '加入候补'
  return activity.value.needApproval ? '申请报名' : '立即报名'
})

const isCreator = computed(() => {
  if (!activity.value?.creatorId || !auth.user?.id) return false
  return String(activity.value.creatorId) === String(auth.user.id)
})
const canCancelActivity = computed(() => isCreator.value && !['CANCELLED', 'FINISHED'].includes(activity.value?.status))
const canFinishActivity = computed(() => isCreator.value && !['CANCELLED', 'FINISHED'].includes(activity.value?.status))
const primaryDisabled = computed(() => {
  if (isCreator.value) return true
  if (['CANCELLED', 'FINISHED'].includes(activity.value?.status)) return true
  return ['APPROVED', 'PENDING', 'WAITING'].includes(activity.value?.signupStatus)
})
const cancelText = computed(() => activity.value?.signupStatus === 'WAITING' ? '取消候补' : '退出')
const chatButtonText = computed(() => chatAccess.value.canAccess ? '进入活动群聊' : '活动群聊暂不可进入')
const canReview = computed(() => {
  if (!activity.value?.endTime || !auth.user?.id) return false
  const ended = new Date(activity.value.endTime).getTime() <= Date.now()
  const isCreator = String(activity.value.creatorId) === String(auth.user.id)
  const isApprovedMember = activity.value.signupStatus === 'APPROVED'
  return ended && (isCreator || isApprovedMember)
})

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

function editActivity() {
  router.push({ path: '/publish', query: { editId: activity.value.id } })
}

async function finishCurrentActivity() {
  try {
    await showConfirmDialog({
      title: '结束活动',
      message: '确认将该活动标记为已结束？'
    })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await finishActivity(activity.value.id)
    showSuccessToast('活动已结束')
    await load()
  } finally {
    actionLoading.value = false
  }
}

async function cancelCurrentActivity() {
  try {
    await showConfirmDialog({
      title: '取消活动',
      message: '取消后用户将无法继续报名，确认取消？'
    })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await cancelActivity(activity.value.id)
    showSuccessToast('活动已取消')
    await load()
  } finally {
    actionLoading.value = false
  }
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

function activityStatusText(status) {
  const map = {
    SIGNING: '报名中',
    FULL: '已满员',
    UPCOMING: '即将开始',
    ONGOING: '进行中',
    FINISHED: '已结束',
    CANCELLED: '已取消'
  }
  return map[status] || status || '-'
}

onMounted(load)
</script>

<style scoped>
.review-entry-tip {
  margin-top: 8px;
}
</style>
