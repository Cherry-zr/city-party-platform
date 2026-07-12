<template>
  <div class="admin-analytics-page">
    <div class="admin-page-heading">
      <div><h1>数据分析</h1><p>按时间范围查看平台增长、结构与业务质量</p></div>
      <el-button :loading="loading" @click="refresh">刷新</el-button>
    </div>

    <section class="admin-card analytics-filter">
      <el-radio-group v-model="period" size="small" @change="handlePeriodChange">
        <el-radio-button v-for="item in periods" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
      </el-radio-group>
      <el-date-picker v-if="period === 'CUSTOM'" v-model="customDates" type="daterange" range-separator="至"
        start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" :clearable="false" @change="refresh" />
      <span class="analytics-range">{{ rangeText }}</span>
    </section>

    <el-alert v-if="validationError || error" class="admin-error-alert" type="error" :title="validationError || error" show-icon :closable="false">
      <template #default><el-button v-if="error" size="small" @click="refresh">重试</el-button></template>
    </el-alert>

    <div class="admin-grid admin-grid-two">
      <ChartPanel title="用户增长" :option="lineOption(trends.users, '新增用户')" :items="trends.users" />
      <ChartPanel title="活动发布" :option="lineOption(trends.activities, '发布活动')" :items="trends.activities" />
      <ChartPanel title="报名趋势" :option="lineOption(trends.signups, '报名')" :items="trends.signups" />
      <ChartPanel title="评价趋势" :option="lineOption(trends.reviews, '评价')" :items="trends.reviews" />
      <ChartPanel title="报名状态" :option="barOption(distributions.signupStatuses)" :items="distributions.signupStatuses" />
      <ChartPanel title="活动状态" :option="barOption(distributions.activityStatuses)" :items="distributions.activityStatuses" />
      <ChartPanel title="活动分类" :option="pieOption(distributions.categories)" :items="distributions.categories" />
      <ChartPanel title="信用分布" :option="barOption(distributions.credits)" :items="distributions.credits" />
      <ChartPanel title="评分分布" :option="barOption(distributions.ratings)" :items="distributions.ratings" />
    </div>

    <section class="admin-card admin-panel analytics-quality">
      <div class="admin-panel-title"><div><h2>业务比率</h2><span>所选时间范围</span></div></div>
      <el-row :gutter="16" v-loading="loading">
        <el-col v-for="item in qualityCards" :key="item.label" :xs="12" :sm="8" :md="4">
          <div class="quality-card"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div>
        </el-col>
      </el-row>
    </section>

    <section class="admin-card admin-panel">
      <div class="admin-panel-title"><div><h2>热门活动</h2><span>最多展示 20 条</span></div></div>
      <el-table v-if="popular.length" :data="popular" stripe v-loading="loading">
        <el-table-column type="index" label="排名" width="70" />
        <el-table-column prop="title" label="活动" min-width="220" show-overflow-tooltip />
        <el-table-column prop="successfulSignups" label="成功报名" width="110" />
        <el-table-column prop="waitlistCount" label="候补人数" width="100" />
      </el-table>
      <el-empty v-else-if="!loading" description="当前范围暂无热门活动" />
    </section>
  </div>
</template>

<script setup>
import axios from 'axios'
import { computed, defineComponent, h, onBeforeUnmount, onMounted, ref } from 'vue'
import AdminChart from '../../components/AdminChart.vue'
import { dashboardDistributions, dashboardPopularActivities, dashboardQuality, dashboardTrends } from '../../api/admin'

const periods = [
  { label: '今天', value: 'TODAY' }, { label: '本周', value: 'THIS_WEEK' }, { label: '本月', value: 'THIS_MONTH' },
  { label: '近 7 天', value: 'LAST_7_DAYS' }, { label: '近 30 天', value: 'LAST_30_DAYS' },
  { label: '近 90 天', value: 'LAST_90_DAYS' }, { label: '本年', value: 'THIS_YEAR' }, { label: '自定义', value: 'CUSTOM' }
]
const period = ref('LAST_30_DAYS')
const customDates = ref([])
const loading = ref(false)
const error = ref('')
const validationError = ref('')
const trends = ref({ users: [], activities: [], signups: [], reviews: [] })
const distributions = ref({ signupStatuses: [], activityStatuses: [], categories: [], credits: [], ratings: [] })
const quality = ref({})
const popular = ref([])
let controller

const rangeText = computed(() => trends.value.startDate ? `${trends.value.startDate} 至 ${trends.value.endDate}` : '请选择时间范围')
const qualityCards = computed(() => [
  { label: '报名成功率', value: `${quality.value.signupSuccessRate ?? 0}%` },
  { label: '平均参与率', value: `${quality.value.averageParticipationRate ?? 0}%` },
  { label: '平均评分', value: quality.value.averageRating ?? 0 },
  { label: '候补数', value: quality.value.waitlistCount ?? 0 },
  { label: '退出数', value: quality.value.exitCount ?? 0 },
  { label: '异常信用用户', value: quality.value.abnormalCreditUserCount ?? 0 }
])

const ChartPanel = defineComponent({
  props: { title: String, option: Object, items: Array },
  setup(props) {
    return () => h('section', { class: 'admin-card admin-panel' }, [
      h('div', { class: 'admin-panel-title' }, [h('div', [h('h2', props.title)])]),
      h(AdminChart, { option: props.option || {}, loading: loading.value, error: Boolean(error.value),
        empty: !(props.items || []).some((item) => Number(item.value) > 0), onRetry: refresh })
    ])
  }
})

function lineOption(items = [], name) {
  return { tooltip: { trigger: 'axis' }, grid: { left: 42, right: 16, top: 24, bottom: 30, containLabel: true },
    xAxis: { type: 'category', data: items.map((item) => item.label), boundaryGap: false }, yAxis: { type: 'value', minInterval: 1 },
    series: [{ name, type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: items.map((item) => item.value), color: '#344054' }] }
}
function barOption(items = []) {
  return { tooltip: { trigger: 'axis' }, grid: { left: 48, right: 16, top: 20, bottom: 45, containLabel: true },
    xAxis: { type: 'category', data: items.map((item) => item.label), axisLabel: { interval: 0, rotate: items.length > 6 ? 25 : 0 } },
    yAxis: { type: 'value', minInterval: 1 }, series: [{ type: 'bar', data: items.map((item) => item.value), color: '#475467', barMaxWidth: 38 }] }
}
function pieOption(items = []) {
  return { tooltip: { trigger: 'item' }, legend: { bottom: 0, type: 'scroll' },
    series: [{ type: 'pie', radius: ['42%', '68%'], center: ['50%', '45%'], data: items.map((item) => ({ name: item.label, value: item.value })),
      color: ['#101828', '#475467', '#667085', '#98a2b3', '#d0d5dd', '#344054', '#7f8c9a'] }] }
}

function params() {
  const value = { period: period.value }
  if (period.value === 'CUSTOM') {
    if (!customDates.value?.[0] || !customDates.value?.[1]) throw new Error('请选择完整的开始和结束日期')
    const start = new Date(`${customDates.value[0]}T00:00:00`)
    const end = new Date(`${customDates.value[1]}T00:00:00`)
    if (start > end) throw new Error('开始日期不得晚于结束日期')
    const max = new Date(start); max.setFullYear(max.getFullYear() + 1)
    if (end > max) throw new Error('自定义时间跨度不得超过一年')
    value.startDate = customDates.value[0]; value.endDate = customDates.value[1]
  }
  return value
}
function handlePeriodChange() {
  validationError.value = ''
  if (period.value !== 'CUSTOM') refresh()
}
async function refresh() {
  let query
  try { query = params(); validationError.value = '' } catch (err) { validationError.value = err.message; return }
  controller?.abort(); controller = new AbortController(); loading.value = true; error.value = ''
  try {
    const config = { signal: controller.signal }
    const [trendData, distributionData, qualityData, popularData] = await Promise.all([
      dashboardTrends(query, config), dashboardDistributions(config), dashboardQuality(query, config),
      dashboardPopularActivities({ ...query, limit: 20 }, config)
    ])
    trends.value = trendData; distributions.value = distributionData; quality.value = qualityData; popular.value = popularData
  } catch (err) {
    if (!axios.isCancel(err)) error.value = err.response?.data?.message || err.message || '分析数据加载失败'
  } finally { if (!controller.signal.aborted) loading.value = false }
}

onMounted(refresh)
onBeforeUnmount(() => controller?.abort())
</script>
