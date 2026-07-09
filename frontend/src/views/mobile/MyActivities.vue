<template>
  <van-nav-bar title="我的活动" left-arrow @click-left="$router.back()" />
  <van-tabs v-model:active="activeType" sticky offset-top="46" @change="handleTabChange">
    <van-tab v-for="tab in tabs" :key="tab.name" :title="tab.title" :name="tab.name" />
  </van-tabs>

  <div class="mobile-content">
    <van-empty v-if="currentState.error" image="error" :description="currentState.error">
      <van-button size="small" type="primary" plain @click="retry">重新加载</van-button>
    </van-empty>

    <van-list
      v-else
      v-model:loading="currentState.loading"
      :finished="currentState.finished"
      :finished-text="currentState.items.length ? '没有更多活动了' : ''"
      @load="loadMore"
    >
      <div v-for="item in currentState.items" :key="item.id" class="plain-panel activity-list-item">
        <div class="activity-list-status">
          <van-tag :type="statusType(item.status)">{{ statusText(item.status, item) }}</van-tag>
          <span v-if="activeType === 'waiting'" class="activity-meta">当前处于候补队列</span>
        </div>
        <ActivityCard :activity="item" @click="$router.push(`/activities/${item.id}`)" />
        <van-button
          v-if="activeType === 'published'"
          size="small"
          type="primary"
          plain
          :loading="signupsLoading && currentActivityId === item.id"
          @click="openSignups(item)"
        >
          查看报名申请
        </van-button>
      </div>
      <van-empty
        v-if="currentState.finished && currentState.items.length === 0"
        :description="emptyDescription"
      />
    </van-list>

    <van-popup v-model:show="showSignups" position="bottom" round :style="{ height: '70%' }">
      <div class="mobile-content signup-popup">
        <div class="section-title">报名申请</div>
        <div v-if="signupsLoading" class="page-state">
          <van-loading vertical>正在加载报名申请...</van-loading>
        </div>
        <van-empty v-else-if="signupsError" image="error" :description="signupsError">
          <van-button size="small" type="primary" plain @click="reloadSignups">重新加载</van-button>
        </van-empty>
        <van-empty v-else-if="signups.length === 0" description="暂无报名" />
        <template v-else>
          <van-cell
            v-for="signup in signups"
            :key="signup.id"
            :title="signup.user?.nickname"
            :label="signup.applyMessage || '无申请说明'"
            :value="signupStatusText(signup.status)"
          >
            <template #right-icon>
              <van-space v-if="signup.status === 'PENDING'">
                <van-button size="mini" type="primary" @click="review(signup.id, 'APPROVED')">同意</van-button>
                <van-button size="mini" type="danger" plain @click="review(signup.id, 'REJECTED')">拒绝</van-button>
              </van-space>
            </template>
          </van-cell>
        </template>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import ActivityCard from '../../components/ActivityCard.vue'
import { activitySignups, myActivities } from '../../api/activity'
import { reviewSignup } from '../../api/signup'

const tabs = [
  { name: 'published', title: '我发布的' },
  { name: 'joined', title: '我参与的' },
  { name: 'waiting', title: '候补中' },
  { name: 'finished', title: '已结束' }
]
const validTypes = new Set(tabs.map((tab) => tab.name))
const route = useRoute()
const router = useRouter()
const initialType = validTypes.has(route.query.type) ? route.query.type : 'published'
const activeType = ref(initialType)
const states = reactive(Object.fromEntries(tabs.map((tab) => [tab.name, createListState()])))
const signups = ref([])
const showSignups = ref(false)
const signupsLoading = ref(false)
const signupsError = ref('')
const currentActivityId = ref(null)

const currentState = computed(() => states[activeType.value])
const emptyDescription = computed(() => {
  const map = {
    published: '暂无发布的活动',
    joined: '暂无参与的活动',
    waiting: '暂无候补中的活动',
    finished: '暂无已结束的活动'
  }
  return map[activeType.value]
})

function createListState() {
  return {
    items: [],
    current: 1,
    size: 10,
    loading: false,
    requesting: false,
    finished: false,
    error: ''
  }
}

async function loadMore() {
  const type = activeType.value
  const state = states[type]
  if (state.finished || state.requesting) return
  state.requesting = true
  state.loading = true
  state.error = ''
  try {
    const data = await myActivities({ type, current: state.current, size: state.size })
    const records = data.records || []
    state.items.push(...records)
    state.current += 1
    state.finished = state.items.length >= data.total || records.length < state.size
  } catch (error) {
    state.error = error.message || '活动加载失败'
    state.finished = true
  } finally {
    state.requesting = false
    state.loading = false
  }
}

function handleTabChange(type) {
  router.replace({ path: '/my-activities', query: { type } })
  const state = states[type]
  if (state.items.length === 0 && !state.finished && !state.loading) {
    loadMore()
  }
}

function retry() {
  states[activeType.value] = createListState()
  loadMore()
}

async function openSignups(activity) {
  currentActivityId.value = activity.id
  showSignups.value = true
  await reloadSignups()
}

async function reloadSignups() {
  signupsLoading.value = true
  signupsError.value = ''
  try {
    const data = await activitySignups(currentActivityId.value, { current: 1, size: 50 })
    signups.value = data.records || []
  } catch (error) {
    signups.value = []
    signupsError.value = error.message || '报名申请加载失败'
  } finally {
    signupsLoading.value = false
  }
}

async function review(id, status) {
  await reviewSignup(id, status)
  showSuccessToast('审核完成')
  await reloadSignups()
  resetCurrentList()
}

function resetCurrentList() {
  states[activeType.value] = createListState()
  loadMore()
}

function statusText(status, activity) {
  if (new Date(activity.endTime).getTime() <= Date.now()) return '已结束'
  const map = {
    SIGNING: '报名中',
    FULL: '已满员',
    UPCOMING: '即将开始',
    ONGOING: '进行中',
    FINISHED: '已结束',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

function statusType(status) {
  if (status === 'CANCELLED') return 'danger'
  if (status === 'FULL') return 'warning'
  if (status === 'SIGNING') return 'primary'
  return 'default'
}

function signupStatusText(status) {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    PROMOTED: '候补转正',
    REJECTED: '已拒绝',
    WAITING: '候补中',
    CANCELLED: '已取消',
    COMPLETED: '已完成'
  }
  return map[status] || status
}
</script>

<style scoped>
.activity-list-item {
  padding: 10px;
}

.activity-list-item :deep(.activity-card) {
  margin-bottom: 10px;
  border: 0;
}

.activity-list-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 2px 10px;
}

.signup-popup {
  min-height: 100%;
}
</style>
