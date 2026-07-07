<template>
  <van-nav-bar title="附近活动地图" left-arrow @click-left="$router.back()" />
  <div class="mobile-content map-page">
    <van-notice-bar v-if="!configured" wrapable :scrollable="false" type="warning">
      未配置高德地图 Key 和安全密钥。请在 frontend/.env.development 中配置 VITE_AMAP_KEY 和 VITE_AMAP_SECURITY_CODE 后重启前端。
    </van-notice-bar>
    <van-notice-bar v-else-if="mapError" wrapable :scrollable="false" type="warning">
      {{ mapError }}
    </van-notice-bar>

    <div class="plain-panel map-filter-panel">
      <van-segmented v-model="distanceKm" :options="distanceOptions" @change="loadNearbyActivities" />
      <div class="activity-meta">{{ locationText }}</div>
    </div>

    <div v-if="configured" ref="mapEl" class="activity-map-container"></div>

    <div v-if="selectedActivity" class="plain-panel">
      <div class="activity-title">{{ selectedActivity.title }}</div>
      <div class="activity-meta">{{ selectedActivity.city }} · {{ selectedActivity.address }}</div>
      <div class="activity-meta">距离约 {{ selectedActivity.distanceKm || '-' }} km · {{ selectedActivity.approvedCount }}/{{ selectedActivity.maxParticipants }} 人</div>
      <van-button size="small" type="primary" @click="$router.push(`/activities/${selectedActivity.id}`)">查看详情</van-button>
    </div>

    <div class="section-title">附近活动</div>
    <van-empty v-if="!loading && activities.length === 0" description="附近暂无可展示活动" />
    <ActivityCard
      v-for="item in activities"
      :key="item.id"
      :activity="item"
      @click="$router.push(`/activities/${item.id}`)"
    />
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import ActivityCard from '../../components/ActivityCard.vue'
import { listNearbyActivities } from '../../api/activity'
import { useAuthStore } from '../../stores/auth'
import { getCityCenter, hasAmapConfig, loadAmap } from '../../utils/amap'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const configured = hasAmapConfig()
const mapEl = ref(null)
const activities = ref([])
const loading = ref(false)
const selectedActivity = ref(null)
const mapError = ref('')
const locationText = ref('正在准备定位信息')
const distanceKm = ref(Number(route.query.distanceKm) || 5)
const distanceOptions = [
  { text: '1km', value: 1 },
  { text: '3km', value: 3 },
  { text: '5km', value: 5 },
  { text: '10km', value: 10 }
]

let AMapInstance = null
let map = null
let infoWindow = null
let markers = []
let currentCenter = [
  Number(route.query.longitude) || getCityCenter(auth.user?.city)[0],
  Number(route.query.latitude) || getCityCenter(auth.user?.city)[1]
]
let currentCity = auth.user?.city || '北京'

onMounted(async () => {
  if (configured) {
    await nextTick()
    await initMap()
  }
  await loadNearbyActivities()
})

async function initMap() {
  try {
    AMapInstance = await loadAmap()
    map = new AMapInstance.Map(mapEl.value, {
      zoom: 13,
      center: currentCenter
    })
    infoWindow = new AMapInstance.InfoWindow({ offset: new AMapInstance.Pixel(0, -30) })
    if (route.query.longitude && route.query.latitude) {
      locationText.value = '已使用活动位置作为地图中心'
      return
    }
    locateUser()
  } catch (error) {
    mapError.value = error.message || '地图加载失败，请检查配置'
    locationText.value = '地图加载失败，已使用默认城市查询活动'
  }
}

function locateUser() {
  if (!AMapInstance || !map) return
  const geolocation = new AMapInstance.Geolocation({
    enableHighAccuracy: true,
    timeout: 8000
  })
  geolocation.getCurrentPosition(async (status, result) => {
    if (status === 'complete' && result?.position) {
      currentCenter = [result.position.lng, result.position.lat]
      currentCity = result.addressComponent?.city || currentCity
      map.setCenter(currentCenter)
      locationText.value = `已定位到当前位置，城市：${currentCity}`
    } else {
      currentCenter = getCityCenter(currentCity)
      map.setCenter(currentCenter)
      locationText.value = `定位失败，已使用 ${currentCity || '北京'} 作为默认城市`
    }
    await loadNearbyActivities()
  })
}

async function loadNearbyActivities() {
  loading.value = true
  try {
    const params = {
      longitude: currentCenter[0],
      latitude: currentCenter[1],
      distanceKm: distanceKm.value,
      current: 1,
      size: 50
    }
    if (!configured && currentCity) {
      delete params.longitude
      delete params.latitude
      params.city = currentCity
    }
    const data = await listNearbyActivities(params)
    activities.value = data.records || []
    renderMarkers()
  } catch (error) {
    showFailToast(error.message || '附近活动加载失败')
  } finally {
    loading.value = false
  }
}

function renderMarkers() {
  if (!AMapInstance || !map) return
  map.remove(markers)
  markers = []
  activities.value
    .filter((item) => item.longitude && item.latitude)
    .forEach((item) => {
      const marker = new AMapInstance.Marker({
        position: [Number(item.longitude), Number(item.latitude)],
        title: item.title
      })
      marker.on('click', () => openActivityInfo(item, marker))
      markers.push(marker)
    })
  if (markers.length > 0) {
    map.add(markers)
    map.setFitView(markers, false, [48, 24, 48, 24])
  }
}

function openActivityInfo(activity, marker) {
  selectedActivity.value = activity
  if (!infoWindow) return
  const content = document.createElement('div')
  content.className = 'map-info-window'
  const title = document.createElement('strong')
  title.textContent = activity.title
  const address = document.createElement('div')
  address.textContent = `${activity.city || ''} ${activity.address || ''}`
  const meta = document.createElement('div')
  meta.textContent = `约 ${activity.distanceKm || '-'} km · ${activity.approvedCount}/${activity.maxParticipants} 人`
  const button = document.createElement('button')
  button.type = 'button'
  button.textContent = '查看详情'
  button.onclick = () => router.push(`/activities/${activity.id}`)
  content.appendChild(title)
  content.appendChild(address)
  content.appendChild(meta)
  content.appendChild(button)
  infoWindow.setContent(content)
  infoWindow.open(map, marker.getPosition())
}
</script>
