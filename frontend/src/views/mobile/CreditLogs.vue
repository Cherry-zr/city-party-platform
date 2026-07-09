<template>
  <van-nav-bar title="信用分明细" left-arrow @click-left="$router.back()" />
  <div class="mobile-content">
    <div class="credit-summary">
      <div class="credit-label">当前信用分</div>
      <div class="credit-score">{{ auth.user?.creditScore ?? '--' }}</div>
      <div class="activity-meta">信用分范围为 60–120</div>
    </div>
    <div class="section-title">变更记录</div>
    <van-empty v-if="!loading && logs.length === 0" description="暂无信用分记录" />
    <div v-for="item in logs" :key="item.id" class="plain-panel credit-log">
      <div>
        <div class="credit-reason">{{ item.reason }}</div>
        <div class="activity-meta">{{ formatDateTime(item.createdAt) }}</div>
      </div>
      <div class="credit-change" :class="{ negative: item.changeValue < 0 }">
        {{ formatDelta(item.changeValue) }}
        <small>{{ item.beforeScore }} → {{ item.afterScore }}</small>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getCreditLogs } from '../../api/credit'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/format'

const auth = useAuthStore()
const logs = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const [, data] = await Promise.all([
      auth.refreshMe(),
      getCreditLogs({ current: 1, size: 50 })
    ])
    logs.value = data.records || []
  } catch {
    logs.value = []
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
  border: 1px solid #e2e5e9;
  border-radius: 8px;
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
  font-size: 44px;
  font-weight: 700;
}

.credit-log {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.credit-reason {
  margin-bottom: 4px;
  color: #303943;
  font-size: 14px;
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
