<template>
  <van-nav-bar title="信用中心" left-arrow @click-left="$router.back()" />
  <div class="mobile-content">
    <div v-if="loading" class="page-state">
      <van-loading vertical>正在加载信用信息...</van-loading>
    </div>

    <van-empty v-else-if="errorMessage" image="error" :description="errorMessage">
      <van-button size="small" type="primary" plain @click="load">重新加载</van-button>
    </van-empty>

    <template v-else-if="overview">
      <div class="credit-summary">
        <div class="credit-label">当前信用分</div>
        <div class="credit-score">{{ overview.creditScore ?? '--' }}</div>
        <van-tag type="success" size="medium">{{ overview.creditLevel }}</van-tag>
        <div class="activity-meta credit-range">信用分范围为 60–120</div>
      </div>

      <div class="section-title">信用分变化记录</div>
      <van-empty v-if="logs.length === 0" description="暂无信用分记录" />
      <template v-else>
        <div v-for="item in logs" :key="item.id" class="plain-panel credit-log">
          <div class="credit-log-main">
            <div class="credit-reason">{{ item.reason }}</div>
            <button
              v-if="item.relatedActivityId"
              type="button"
              class="activity-link"
              @click="$router.push(`/activities/${item.relatedActivityId}`)"
            >
              关联活动：{{ item.relatedActivityTitle }}
            </button>
            <div class="activity-meta">{{ formatDateTime(item.createdAt) }}</div>
          </div>
          <div class="credit-change" :class="{ negative: item.changeValue < 0 }">
            {{ formatDelta(item.changeValue) }}
            <small>{{ item.beforeScore }} → {{ item.afterScore }}</small>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getCreditOverview } from '../../api/credit'
import { formatDateTime } from '../../utils/format'

const overview = ref(null)
const logs = ref([])
const loading = ref(false)
const errorMessage = ref('')

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getCreditOverview({ current: 1, size: 50 })
    overview.value = data
    logs.value = data.records?.records || []
  } catch (error) {
    overview.value = null
    logs.value = []
    errorMessage.value = error.message || '信用信息加载失败'
  } finally {
    loading.value = false
  }
}

function formatDelta(value) {
  if (!value) return '0'
  return value > 0 ? `+${value}` : String(value)
}

onMounted(load)
</script>

<style scoped>
.credit-summary {
  padding: 24px;
  border: 1px solid #dfe3e6;
  border-radius: 12px;
  background: #fff;
  text-align: center;
}

.credit-label {
  color: #5f6b7a;
  font-size: 14px;
}

.credit-score {
  margin: 8px 0;
  color: #1f2933;
  font-size: 46px;
  font-weight: 700;
}

.credit-range {
  margin-top: 10px;
}

.credit-log {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.credit-log-main {
  flex: 1;
  min-width: 0;
}

.credit-reason {
  margin-bottom: 6px;
  color: #303943;
  font-size: 14px;
  line-height: 1.55;
}

.activity-link {
  display: block;
  max-width: 100%;
  margin: 0 0 6px;
  overflow: hidden;
  border: 0;
  background: transparent;
  padding: 0;
  color: #57626e;
  font: inherit;
  font-size: 13px;
  text-align: left;
  text-decoration: underline;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.credit-change {
  flex: none;
  color: #168443;
  font-size: 20px;
  font-weight: 700;
  text-align: right;
}

.credit-change.negative {
  color: #c43d3d;
}

.credit-change small {
  display: block;
  margin-top: 4px;
  color: #7b8490;
  font-size: 12px;
  font-weight: 400;
}
</style>
