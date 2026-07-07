<template>
  <van-nav-bar title="发布活动" />
  <div class="mobile-content">
    <van-form @submit="submit">
      <div class="plain-panel">
        <van-field v-model="form.title" label="标题" placeholder="请输入活动标题" required />
        <van-field v-model="form.category" label="分类" is-link readonly @click="showCategory = true" required />
        <van-field v-model="tagText" label="标签" placeholder="AA制,周末,轻社交" />
        <van-field v-model="form.startTime" label="开始时间" placeholder="2026-08-01T19:00:00" required />
        <van-field v-model="form.endTime" label="结束时间" placeholder="2026-08-01T22:00:00" required />
        <van-field v-model="form.signupDeadline" label="截止时间" placeholder="2026-08-01T12:00:00" required />
        <van-field v-model="form.city" label="城市" required />
        <van-field v-model="form.address" label="地址" required />
        <van-field v-model.number="form.longitude" label="经度" type="number" placeholder="可手动填写或地图选点" />
        <van-field v-model.number="form.latitude" label="纬度" type="number" placeholder="可手动填写或地图选点" />
        <van-cell title="地图选点" :label="locationSummary">
          <template #right-icon>
            <van-button size="small" plain type="primary" native-type="button" @click.stop="showPicker = true">选择地点</van-button>
          </template>
        </van-cell>
        <van-field v-model.number="form.minParticipants" label="最小人数" type="number" required />
        <van-field v-model.number="form.maxParticipants" label="最大人数" type="number" required />
        <van-field v-model="form.costType" label="费用类型" is-link readonly @click="showCost = true" required />
        <van-field v-model.number="form.costAmount" label="费用金额" type="number" />
        <van-field v-model="form.aaRule" label="AA规则" />
        <van-field v-model="form.description" label="说明" type="textarea" required />
        <van-field v-model="form.notes" label="注意事项" type="textarea" />
        <van-cell title="需要审核">
          <template #right-icon>
            <van-switch v-model="form.needApproval" size="20" />
          </template>
        </van-cell>
      </div>
      <van-button block type="primary" native-type="submit" :loading="loading">发布</van-button>
    </van-form>
    <van-action-sheet v-model:show="showCategory" :actions="categories.map(name => ({ name }))" @select="selectCategory" />
    <van-action-sheet v-model:show="showCost" :actions="costs" @select="selectCost" />
    <AmapLocationPicker
      v-model:show="showPicker"
      :initial-city="form.city"
      :initial-address="form.address"
      :initial-longitude="form.longitude"
      :initial-latitude="form.latitude"
      @select="selectLocation"
    />
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { createActivity } from '../../api/activity'
import AmapLocationPicker from '../../components/AmapLocationPicker.vue'

const router = useRouter()
const loading = ref(false)
const showCategory = ref(false)
const showCost = ref(false)
const showPicker = ref(false)
const tagText = ref('AA制,新手友好')
const categories = ['观影', '聚餐', '运动', '桌游', '学习', '探店', '户外', '游戏', '展览', '其他']
const costs = [
  { name: '免费', value: 'FREE' },
  { name: 'AA制', value: 'AA' },
  { name: '固定费用', value: 'FIXED' },
  { name: '预估费用', value: 'ESTIMATE' }
]
const form = reactive({
  title: '',
  category: '观影',
  startTime: '2026-08-01T19:00:00',
  endTime: '2026-08-01T22:00:00',
  signupDeadline: '2026-08-01T12:00:00',
  city: '北京',
  address: '',
  longitude: 116.4,
  latitude: 39.9,
  minParticipants: 2,
  maxParticipants: 6,
  costType: 'AA',
  costAmount: 50,
  aaRule: '现场按实际费用 AA',
  coverUrl: '',
  description: '',
  notes: '',
  needApproval: false
})

const locationSummary = computed(() => {
  if (!form.longitude || !form.latitude) return '未选择坐标'
  return `${form.longitude}, ${form.latitude}`
})

function selectCategory(item) {
  form.category = item.name
  showCategory.value = false
}

function selectCost(item) {
  form.costType = item.value
  showCost.value = false
}

function selectLocation(location) {
  form.city = location.city || form.city
  form.address = location.address || form.address
  form.longitude = location.longitude
  form.latitude = location.latitude
}

async function submit() {
  loading.value = true
  try {
    const data = await createActivity({ ...form, tags: tagText.value.split(',').map((item) => item.trim()).filter(Boolean) })
    showSuccessToast('发布成功')
    router.push(`/activities/${data.id}`)
  } finally {
    loading.value = false
  }
}
</script>
