<template>
  <div class="admin-dashboard-page">
    <div class="admin-page-heading">
      <div>
        <h1>运营概览</h1>
        <p>平台核心数据、近期趋势和需要关注的运营指标</p>
      </div>
      <el-space>
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="$router.push('/admin/analytics')">详细分析</el-button>
      </el-space>
    </div>

    <el-alert v-if="error" class="admin-error-alert" type="error" :title="error" show-icon :closable="false">
      <template #default><el-button size="small" @click="load">重新加载</el-button></template>
    </el-alert>

    <el-row :gutter="16">
      <el-col v-for="item in cards" :key="item.label" :xs="12" :sm="12" :md="6">
        <div class="admin-card dashboard-card" v-loading="loading">
          <div class="dashboard-label">{{ item.label }}</div>
          <div class="dashboard-value">{{ item.value }}</div>
          <div class="dashboard-delta">今日新增 {{ item.today }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="admin-grid admin-grid-two">
      <section class="admin-card admin-panel">
        <div class="admin-panel-title"><div><h2>用户增长</h2><span>最近 7 天</span></div></div>
        <AdminChart :option="userOption" :loading="loading" :empty="!hasTrend(trends.users)" :error="Boolean(error)" @retry="load" />
      </section>
      <section class="admin-card admin-panel">
        <div class="admin-panel-title"><div><h2>活动发布</h2><span>最近 7 天</span></div></div>
        <AdminChart :option="activityOption" :loading="loading" :empty="!hasTrend(trends.activities)" :error="Boolean(error)" @retry="load" />
      </section>
    </div>

    <div class="admin-grid admin-grid-wide">
      <section class="admin-card admin-panel">
        <div class="admin-panel-title"><div><h2>热门活动</h2><span>按成功报名与候补排序</span></div></div>
        <el-table v-if="popular.length" :data="popular" size="small" stripe>
          <el-table-column type="index" label="#" width="48" />
          <el-table-column prop="title" label="活动" min-width="180" show-overflow-tooltip />
          <el-table-column prop="successfulSignups" label="成功报名" width="92" />
          <el-table-column prop="waitlistCount" label="候补" width="72" />
        </el-table>
        <el-empty v-else-if="!loading" description="暂无热门活动" :image-size="72" />
      </section>
      <section class="admin-card admin-panel">
        <div class="admin-panel-title"><div><h2>待关注指标</h2><span>最近 30 天</span></div></div>
        <div class="attention-list" v-loading="loading">
          <div v-for="item in attention" :key="item.label" class="attention-item">
            <span>{{ item.label }}</span><strong :class="item.tone">{{ item.value }}</strong>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import axios from 'axios'
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref } from 'vue'
import { dashboardOverview, dashboardPopularActivities, dashboardQuality, dashboardTrends } from '../../api/admin'

const AdminChart = defineAsyncComponent(() => import('../../components/AdminChart.vue'))

const loading = ref(false)
const error = ref('')
const overview = ref({})
const trends = ref({ users: [], activities: [] })
const quality = ref({})
const popular = ref([])
let controller

const cards = computed(() => [
  { label: '用户总数', value: overview.value.userCount ?? 0, today: overview.value.todayUsers ?? 0 },
  { label: '活动总数', value: overview.value.activityCount ?? 0, today: overview.value.todayActivities ?? 0 },
  { label: '报名总数', value: overview.value.signupCount ?? 0, today: overview.value.todaySignups ?? 0 },
  { label: '评价总数', value: overview.value.reviewCount ?? 0, today: overview.value.todayReviews ?? 0 }
])
const attention = computed(() => [
  { label: '报名成功率', value: `${quality.value.signupSuccessRate ?? 0}%`, tone: 'positive-score' },
  { label: '平均参与率', value: `${quality.value.averageParticipationRate ?? 0}%`, tone: '' },
  { label: '平均评分', value: quality.value.averageRating ?? 0, tone: 'positive-score' },
  { label: '当前候补', value: quality.value.waitlistCount ?? 0, tone: '' },
  { label: '退出报名', value: quality.value.exitCount ?? 0, tone: 'negative-score' },
  { label: '异常信用用户', value: quality.value.abnormalCreditUserCount ?? 0, tone: 'negative-score' }
])
const lineOption = (items, name) => ({
  tooltip: { trigger: 'axis' }, grid: { left: 40, right: 16, top: 24, bottom: 30, containLabel: true },
  xAxis: { type: 'category', data: items.map((item) => item.label), boundaryGap: false },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{ name, type: 'line', smooth: true, symbolSize: 7, areaStyle: { opacity: 0.08 }, data: items.map((item) => item.value), color: '#344054' }]
})
const userOption = computed(() => lineOption(trends.value.users || [], '新增用户'))
const activityOption = computed(() => lineOption(trends.value.activities || [], '发布活动'))

function hasTrend(items = []) { return items.some((item) => Number(item.value) > 0) }

async function load() {
  controller?.abort()
  controller = new AbortController()
  loading.value = true
  error.value = ''
  try {
    const config = { signal: controller.signal }
    const [overviewData, trendData, qualityData, popularData] = await Promise.all([
      dashboardOverview(config), dashboardTrends({ period: 'LAST_7_DAYS' }, config),
      dashboardQuality({ period: 'LAST_30_DAYS' }, config), dashboardPopularActivities({ period: 'LAST_30_DAYS', limit: 6 }, config)
    ])
    overview.value = overviewData
    trends.value = trendData
    quality.value = qualityData
    popular.value = popularData
  } catch (err) {
    if (!axios.isCancel(err)) error.value = err.response?.data?.message || err.message || '运营概览加载失败'
  } finally {
    if (!controller.signal.aborted) loading.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => controller?.abort())
</script>
