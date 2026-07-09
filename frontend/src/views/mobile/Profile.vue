<template>
  <van-nav-bar title="个人中心" />
  <div class="mobile-content profile-page">
    <div v-if="loading" class="page-state">
      <van-loading vertical>正在加载个人信息...</van-loading>
    </div>

    <van-empty v-else-if="errorMessage" image="error" :description="errorMessage">
      <van-button size="small" type="primary" plain @click="load">重新加载</van-button>
    </van-empty>

    <template v-else-if="overview">
      <section class="profile-hero">
        <div class="profile-main">
          <van-image round width="68" height="68" :src="overview.avatarUrl || avatarFallback" />
          <div class="profile-identity">
            <div class="profile-name">{{ overview.nickname || overview.username }}</div>
            <div class="activity-meta">{{ overview.username }} · {{ overview.city || '暂未填写城市' }}</div>
          </div>
          <van-button size="small" plain to="/profile/edit">编辑资料</van-button>
        </div>
        <p v-if="overview.bio" class="profile-bio">{{ overview.bio }}</p>
        <div v-if="overview.interestTags?.length" class="tag-row">
          <van-tag v-for="tag in overview.interestTags" :key="tag" plain>{{ tag }}</van-tag>
        </div>
        <div class="credit-card" @click="router.push('/credit/logs')">
          <div>
            <div class="credit-card-label">当前信用分</div>
            <div class="credit-card-score">{{ overview.creditScore ?? '--' }}</div>
          </div>
          <div class="credit-card-level">
            <van-tag type="success" size="medium">{{ overview.creditLevel }}</van-tag>
            <span>查看信用明细</span>
          </div>
        </div>
      </section>

      <section class="plain-panel profile-stat-panel">
        <div class="section-title compact-title">我的数据</div>
        <van-grid :column-num="3" :border="false" :gutter="8">
          <van-grid-item v-for="item in statistics" :key="item.label" @click="item.to && router.push(item.to)">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </van-grid-item>
        </van-grid>
      </section>

      <van-cell-group inset>
        <van-cell title="我的活动" label="发布、参与、候补和已结束活动" icon="calendar-o" is-link to="/my-activities" />
        <van-cell title="我的报名" label="查看全部报名申请状态" icon="friends-o" is-link to="/my-signups" />
        <van-cell title="我的评价" label="我发出的和收到的评价" icon="star-o" is-link to="/reviews/my" />
        <van-cell title="信用中心" :value="`${overview.creditScore} 分 · ${overview.creditLevel}`" icon="shield-o" is-link to="/credit/logs" />
        <van-cell title="通知中心" :value="noticeValue" icon="bell" is-link to="/notices" />
        <van-cell title="我的收藏" icon="like-o" is-link to="/my-favorites" />
        <van-cell v-if="auth.isAdmin" title="管理员后台" icon="manager-o" is-link to="/admin/dashboard" />
      </van-cell-group>

      <van-button class="logout-button" block type="danger" plain @click="logout">退出登录</van-button>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProfileOverview } from '../../api/user'
import { useAuthStore } from '../../stores/auth'
import { useNoticeStore } from '../../stores/notice'

const router = useRouter()
const auth = useAuthStore()
const notice = useNoticeStore()
const overview = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'

const noticeValue = computed(() => notice.unreadCount > 0 ? `${notice.unreadCount} 条未读` : '暂无未读')

const statistics = computed(() => [
  { label: '发布活动', value: overview.value?.publishedActivityCount ?? 0, to: '/my-activities?type=published' },
  { label: '参与活动', value: overview.value?.joinedActivityCount ?? 0, to: '/my-activities?type=joined' },
  { label: '候补中', value: overview.value?.waitingActivityCount ?? 0, to: '/my-activities?type=waiting' },
  { label: '收到评价', value: overview.value?.receivedReviewCount ?? 0, to: '/reviews/my?type=received' },
  { label: '平均评分', value: overview.value?.averageRating ?? '--', to: '/reviews/my?type=received' },
  { label: '未读通知', value: notice.unreadCount, to: '/notices' }
])

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    overview.value = await getProfileOverview()
    notice.unreadCount = overview.value.unreadNoticeCount || 0
  } catch (error) {
    errorMessage.value = error.message || '个人信息加载失败'
  } finally {
    loading.value = false
  }
}

function logout() {
  auth.logout()
  router.replace('/login')
}

onMounted(load)
</script>

<style scoped>
.profile-page {
  padding-bottom: 90px;
}

.profile-hero {
  padding: 16px;
  border: 1px solid #e4e7eb;
  border-radius: 12px;
  background: #fff;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-identity {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.profile-name {
  margin-bottom: 5px;
  overflow: hidden;
  font-size: 20px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-main :deep(.van-button) {
  flex: none;
}

.profile-bio {
  margin: 14px 0 8px;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.6;
}

.credit-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #2d3439;
  color: #fff;
}

.credit-card-label {
  color: #d4d8db;
  font-size: 12px;
}

.credit-card-score {
  margin-top: 2px;
  font-size: 30px;
  font-weight: 700;
}

.credit-card-level {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  color: #d4d8db;
  font-size: 12px;
}

.profile-stat-panel {
  margin-top: 12px;
}

.compact-title {
  margin: 0 0 12px;
}

.stat-value {
  color: #20272d;
  font-size: 20px;
  font-weight: 700;
}

.stat-label {
  margin-top: 5px;
  color: #6b7280;
  font-size: 12px;
}

.logout-button {
  margin-top: 16px;
}
</style>
