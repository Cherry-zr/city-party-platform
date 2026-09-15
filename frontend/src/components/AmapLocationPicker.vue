<template>
  <van-popup :show="show" position="bottom" round :style="{ height: '88%' }" @update:show="close">
    <div class="location-picker">
      <van-nav-bar title="选择地点" left-text="取消" right-text="确定" @click-left="close" @click-right="confirm" />
      <div class="location-picker-body">
        <van-notice-bar v-if="!configured" wrapable :scrollable="false" type="warning">
          未配置高德地图 Key 和安全密钥，当前只能手动填写地址和经纬度。
        </van-notice-bar>
        <van-search
          v-model="keyword"
          placeholder="搜索地点或地址"
          :disabled="!configured"
          @update:model-value="handleKeywordInput"
          @search="searchPlace"
        />
        <van-cell-group v-if="suggestions.length" inset class="location-suggestions">
          <van-cell
            v-for="(item, index) in suggestions"
            :key="item.id || item.name + index"
            clickable
            :title="item.name"
            :label="suggestionLabel(item)"
            @click="selectSuggestion(item)"
          />
        </van-cell-group>
        <div v-if="configured" ref="mapEl" class="picker-map"></div>
        <van-cell-group inset>
          <van-field v-model="selected.city" label="城市" placeholder="例如：北京" />
          <van-field v-model="selected.address" label="地址" placeholder="请输入详细地址" />
          <van-field v-model.number="selected.longitude" label="经度" type="number" placeholder="例如：116.397428" />
          <van-field v-model.number="selected.latitude" label="纬度" type="number" placeholder="例如：39.909230" />
        </van-cell-group>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { showFailToast, showToast } from 'vant'
import { getCityCenter, hasAmapConfig, loadAmap } from '../utils/amap'

const props = defineProps({
  show: {
    type: Boolean,
    required: true
  },
  initialCity: {
    type: String,
    default: '北京'
  },
  initialAddress: {
    type: String,
    default: ''
  },
  initialLongitude: {
    type: [Number, String],
    default: null
  },
  initialLatitude: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['update:show', 'select'])

const configured = hasAmapConfig() || Boolean(window.AMap)
const mapEl = ref(null)
const keyword = ref('')
const suggestions = ref([])
const selected = reactive({
  city: '北京',
  address: '',
  longitude: null,
  latitude: null
})

let AMapInstance = null
let map = null
let marker = null
let placeSearch = null
let autoComplete = null
let geocoder = null
let suggestionTimer = null

watch(() => props.show, async (visible) => {
  if (!visible) return
  syncInitial()
  if (configured) {
    await nextTick()
    await initMap()
  }
})

watch(() => selected.city, syncSearchCity)

function syncInitial() {
  selected.city = props.initialCity || '北京'
  selected.address = props.initialAddress || ''
  selected.longitude = validCoordinate(props.initialLongitude) ? Number(props.initialLongitude) : null
  selected.latitude = validCoordinate(props.initialLatitude) ? Number(props.initialLatitude) : null
  keyword.value = selected.address
  suggestions.value = []
}

async function initMap() {
  try {
    AMapInstance = await loadAmap()
    const center = hasSelectedPoint()
      ? [Number(selected.longitude), Number(selected.latitude)]
      : getCityCenter(selected.city)
    if (!map) {
      map = new AMapInstance.Map(mapEl.value, {
        zoom: 14,
        center
      })
      geocoder = new AMapInstance.Geocoder()
      placeSearch = new AMapInstance.PlaceSearch({
        city: selected.city || '全国',
        citylimit: true,
        pageSize: 8
      })
      autoComplete = new AMapInstance.AutoComplete({
        city: selected.city || '全国',
        citylimit: true
      })
      map.on('click', (event) => choosePoint(event.lnglat.getLng(), event.lnglat.getLat()))
    } else {
      map.setCenter(center)
    }
    syncSearchCity()
    if (hasSelectedPoint()) {
      drawMarker(center)
    } else if (marker) {
      map.remove(marker)
      marker = null
    }
  } catch (error) {
    showFailToast(error.message || '地图加载失败')
  }
}

function drawMarker(position) {
  if (!AMapInstance || !map) return
  if (!marker) {
    marker = new AMapInstance.Marker({ position })
    map.add(marker)
  } else {
    marker.setPosition(position)
  }
  map.setCenter(position)
}

function choosePoint(longitude, latitude) {
  selected.longitude = Number(longitude.toFixed(6))
  selected.latitude = Number(latitude.toFixed(6))
  drawMarker([selected.longitude, selected.latitude])
  if (!geocoder) return
  geocoder.getAddress([selected.longitude, selected.latitude], (status, result) => {
    if (status !== 'complete' || !result?.regeocode) return
    selected.address = result.regeocode.formattedAddress || selected.address
    selected.city = result.regeocode.addressComponent?.city || result.regeocode.addressComponent?.province || selected.city
  })
}

function handleKeywordInput(value) {
  clearTimeout(suggestionTimer)
  if (!value?.trim() || value.trim().length < 2) {
    suggestions.value = []
    return
  }
  suggestionTimer = setTimeout(() => loadSuggestions(value.trim()), 250)
}

function loadSuggestions(value) {
  if (!autoComplete) return
  syncSearchCity()
  autoComplete.search(value, (status, result) => {
    suggestions.value = status === 'complete'
      ? (result?.tips || []).filter((item) => item?.name)
      : []
  })
}

function searchPlace() {
  const value = keyword.value.trim()
  if (!value || !placeSearch) return
  syncSearchCity()
  placeSearch.search(value, (status, result) => {
    const pois = status === 'complete' ? result?.poiList?.pois || [] : []
    suggestions.value = pois
    if (pois.length === 0) {
      showToast('当前城市没有找到匹配地点')
    }
  })
}

function selectSuggestion(item) {
  const coordinates = coordinatesOf(item.location)
  if (coordinates) {
    applyPoi(item, coordinates)
    return
  }
  if (!placeSearch) return
  syncSearchCity()
  placeSearch.search(item.name, (status, result) => {
    const poi = result?.poiList?.pois?.[0]
    const poiCoordinates = coordinatesOf(poi?.location)
    if (status !== 'complete' || !poi || !poiCoordinates) {
      showToast('该地点暂无可用坐标')
      return
    }
    applyPoi(poi, poiCoordinates)
  })
}

function applyPoi(poi, coordinates) {
  selected.city = poi.cityname || selected.city
  selected.address = [poi.name, poi.address].filter(Boolean).join(' ')
  selected.longitude = Number(coordinates[0].toFixed(6))
  selected.latitude = Number(coordinates[1].toFixed(6))
  keyword.value = poi.name || selected.address
  suggestions.value = []
  drawMarker([selected.longitude, selected.latitude])
}

function coordinatesOf(location) {
  if (!location) return null
  const longitude = Number(location.lng ?? location.getLng?.())
  const latitude = Number(location.lat ?? location.getLat?.())
  return Number.isFinite(longitude) && Number.isFinite(latitude)
    ? [longitude, latitude]
    : null
}

function suggestionLabel(item) {
  return [item.district, item.address].filter(Boolean).join(' ') || selected.city
}

function syncSearchCity() {
  const city = selected.city || '全国'
  placeSearch?.setCity(city)
  autoComplete?.setCity(city)
}

function validCoordinate(value) {
  return value !== null && value !== '' && Number.isFinite(Number(value))
}

function hasSelectedPoint() {
  return validCoordinate(selected.longitude) && validCoordinate(selected.latitude)
}

function confirm() {
  if (!hasSelectedPoint() || !selected.address?.trim()) {
    showFailToast('请先从联想结果选择地点，或在地图上点击选点')
    return
  }
  emit('select', { ...selected })
  close()
}

function close() {
  emit('update:show', false)
}

onBeforeUnmount(() => clearTimeout(suggestionTimer))
</script>

<style scoped>
.location-suggestions {
  position: relative;
  z-index: 2;
  max-height: 220px;
  margin-bottom: 10px;
  overflow-y: auto;
  border: 1px solid #ebedf0;
}
</style>
