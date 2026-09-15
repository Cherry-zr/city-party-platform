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
      <div class="distance-segmented" role="radiogroup" aria-label="附近活动距离筛选">
        <button
          v-for="option in distanceOptions"
          :key="option.value"
          type="button"
          class="distance-segmented__item"
          :class="{ 'distance-segmented__item--active': distanceKm === option.value }"
          :aria-checked="distanceKm === option.value"
          role="radio"
          @click="selectDistance(option.value)"
        >
          {{ option.text }}
        </button>
      </div>
      <div class="activity-meta">{{ locationText }}</div>
      <div class="map-location-actions">
        <van-button
          data-testid="locate-current-city"
          size="small"
          type="primary"
          :loading="locating"
          @click="locateUser"
        >
          定位当前城市
        </van-button>
        <van-button size="small" plain @click="showCityPicker = true">手动选择城市</van-button>
      </div>
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
    <van-action-sheet
      v-model:show="showCityPicker"
      :actions="cityOptions"
      cancel-text="取消"
      @select="selectManualCity"
    />
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import ActivityCard from '../../components/ActivityCard.vue'
import { listNearbyActivities } from '../../api/activity'
import { cityCenters, hasAmapConfig, loadAmap } from '../../utils/amap'

const route = useRoute()
const router = useRouter()
const configured = hasAmapConfig() || Boolean(window.AMap)
const mapEl = ref(null)
const activities = ref([])
const loading = ref(false)
const selectedActivity = ref(null)
const mapError = ref('')
const locationText = ref('正在准备定位信息')
const locating = ref(false)
const showCityPicker = ref(false)
const distanceKm = ref(Number(route.query.distanceKm) || 5)
const distanceOptions = [
  { text: '1km', value: 1 },
  { text: '3km', value: 3 },
  { text: '5km', value: 5 },
  { text: '10km', value: 10 }
]
const cityOptions = Object.keys(cityCenters).map((name) => ({ name }))

let AMapInstance = null
let map = null
let infoWindow = null
let geocoder = null
let markers = []
let currentCenter = null
let currentCity = ''

onMounted(async () => {
  if (configured) {
    await nextTick()
    await initMap()
  }
  await locateUser()
})

async function initMap() {
  try {
    AMapInstance = await loadAmap()
    map = new AMapInstance.Map(mapEl.value, { zoom: 13 })
    infoWindow = new AMapInstance.InfoWindow({ offset: new AMapInstance.Pixel(0, -30) })
    geocoder = new AMapInstance.Geocoder()
  } catch (error) {
    mapError.value = error.message || '地图加载失败，请检查配置'
    locationText.value = '地图加载失败，仍可使用浏览器定位或手动选择城市'
  }
}

async function locateUser() {
  if (locating.value) return
  if (!navigator.geolocation) {
    handleLocationFailure('当前浏览器不支持定位，请手动选择城市')
    return
  }
  if ('isSecureContext' in window && !window.isSecureContext) {
    handleLocationFailure('当前环境不允许定位，请使用 HTTPS 或 localhost 后重试')
    return
  }
  locating.value = true
  locationText.value = '正在获取真实位置...'
  try {
    const position = await new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      })
    })
    const longitude = Number(position.coords.longitude)
    const latitude = Number(position.coords.latitude)
    currentCenter = [longitude, latitude]
    currentCity = await reverseGeocodeCity(currentCenter)
    map?.setCenter(currentCenter)
    locationText.value = currentCity
      ? '已定位到当前位置，城市：' + currentCity
      : '已定位到当前位置，但暂时无法解析城市'
    await loadNearbyActivities()
  } catch (error) {
    handleLocationFailure(locationErrorMessage(error))
  } finally {
    locating.value = false
  }
}

function reverseGeocodeCity(position) {
  if (!geocoder) return Promise.resolve('')
  return new Promise((resolve) => {
    geocoder.getAddress(position, (status, result) => {
      if (status !== 'complete' || !result?.regeocode) {
        resolve('')
        return
      }
      const component = result.regeocode.addressComponent || {}
      resolve(component.city || component.province || '')
    })
  })
}

function handleLocationFailure(message) {
  currentCenter = null
  currentCity = ''
  activities.value = []
  selectedActivity.value = null
  renderMarkers()
  locationText.value = message
  showFailToast(message)
}

function locationErrorMessage(error) {
  if (error?.code === 1) return '定位未授权，请允许定位后重试或手动选择城市'
  if (error?.code === 2) return '当前位置不可用，请检查系统定位服务或手动选择城市'
  if (error?.code === 3) return '定位超时，请重新定位或手动选择城市'
  return '定位失败，请重新定位或手动选择城市'
}

async function selectManualCity(item) {
  const center = cityCenters[item.name]
  if (!center) return
  showCityPicker.value = false
  currentCity = item.name
  currentCenter = [...center]
  map?.setCenter(currentCenter)
  locationText.value = '已手动选择城市：' + currentCity
  await loadNearbyActivities()
}

async function loadNearbyActivities() {
  if (!currentCenter) {
    activities.value = []
    return
  }
  loading.value = true
  try {
    const params = {
      longitude: currentCenter[0],
      latitude: currentCenter[1],
      distanceKm: distanceKm.value,
      current: 1,
      size: 50
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

async function selectDistance(value) {
  if (distanceKm.value === value) return
  distanceKm.value = value
  await loadNearbyActivities()
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

<style scoped>
.distance-segmented {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 4px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f4f5f7;
}

.distance-segmented__item {
  min-width: 0;
  min-height: 34px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #5f6b7a;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
}

.distance-segmented__item--active {
  background: #fff;
  color: #1f2933;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(31, 41, 51, 0.12);
}

.distance-segmented__item:focus-visible {
  outline: 2px solid #969da8;
  outline-offset: 2px;
}

.map-location-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
</style>
