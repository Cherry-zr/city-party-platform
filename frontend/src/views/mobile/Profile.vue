<template>
  <van-nav-bar title="个人中心" />
  <div class="mobile-content">
    <div class="plain-panel">
      <van-space>
        <van-image round width="64" height="64" :src="auth.user?.avatarUrl || avatarFallback" />
        <div>
          <div class="activity-title">{{ auth.user?.nickname }}</div>
          <div class="activity-meta">{{ auth.user?.username }} · {{ auth.user?.city }} · 信用分 {{ auth.user?.creditScore }}</div>
        </div>
      </van-space>
    </div>
    <van-cell-group inset>
      <van-cell title="编辑资料" is-link to="/profile/edit" />
      <van-cell title="我的活动" is-link to="/my-activities" />
      <van-cell title="我的报名" is-link to="/my-signups" />
      <van-cell title="我的收藏" is-link to="/my-favorites" />
      <van-cell title="我的搭子" value="开发中" is-link to="/partner" />
      <van-cell title="我的评价" value="第二阶段" />
      <van-cell title="AA账单" value="第二阶段" />
      <van-cell title="系统通知" :value="noticeValue" is-link to="/notices" />
      <van-cell v-if="auth.isAdmin" title="管理员后台" is-link to="/admin/dashboard" />
    </van-cell-group>
    <div class="mobile-content">
      <van-button block type="danger" plain @click="logout">退出登录</van-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useNoticeStore } from '../../stores/notice'

const router = useRouter()
const auth = useAuthStore()
const notice = useNoticeStore()
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'

const noticeValue = computed(() => notice.unreadCount > 0 ? `${notice.unreadCount} 条未读` : '无未读')

function logout() {
  auth.logout()
  router.replace('/login')
}

onMounted(() => {
  if (auth.isLogin) {
    notice.loadUnreadCount()
  }
})
</script>
