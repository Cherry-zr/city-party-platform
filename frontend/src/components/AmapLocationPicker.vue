<template>
  <van-popup :show="show" position="bottom" round :style="{ height: '88%' }" @update:show="close">
    <div class="location-picker">
      <van-nav-bar title="选择地点" left-text="取消" right-text="确定" @click-left="close" @click-right="confirm" />
      <div class="location-picker-body">
        <van-notice-bar v-if="!configured" wrapable :scrollable="false" type="warning">
          未配置高德地图 Key 和安全密钥，当前只能手动填写地址和经纬度。
        </van-notice-bar>
        <van-search v-model="keyword" placeholder="搜索地点或地址" :disabled="!configured" @search="searchPlace" />
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
import { nextTick, reactive, ref, watch } from 'vue'
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
    default: 116.397428
  },
  initialLatitude: {
    type: [Number, String],
    default: 39.90923
  }
})

const emit = defineEmits(['update:show', 'select'])

const configured = hasAmapConfig()
const mapEl = ref(null)
const keyword = ref('')
const selected = reactive({
  city: '北京',
  address: '',
  longitude: 116.397428,
  latitude: 39.90923
})

let AMapInstance = null
let map = null
let marker = null
let placeSearch = null
let geocoder = null

watch(() => props.show, async (visible) => {
  if (!visible) return
  syncInitial()
  if (configured) {
    await nextTick()
    await initMap()
  }
})

function syncInitial() {
  selected.city = props.initialCity || '北京'
  selected.address = props.initialAddress || ''
  selected.longitude = Number(props.initialLongitude) || getCityCenter(selected.city)[0]
  selected.latitude = Number(props.initialLatitude) || getCityCenter(selected.city)[1]
  keyword.value = selected.address
}

async function initMap() {
  try {
    AMapInstance = await loadAmap()
    const center = [Number(selected.longitude), Number(selected.latitude)]
    if (!map) {
      map = new AMapInstance.Map(mapEl.value, {
        zoom: 14,
        center
      })
      geocoder = new AMapInstance.Geocoder()
      placeSearch = new AMapInstance.PlaceSearch({
        city: selected.city || '全国',
        pageSize: 8
      })
      map.on('click', (event) => choosePoint(event.lnglat.getLng(), event.lnglat.getLat()))
    } else {
      map.setCenter(center)
    }
    drawMarker(center)
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

function searchPlace() {
  if (!keyword.value.trim() || !placeSearch) return
  placeSearch.setCity(selected.city || '全国')
  placeSearch.search(keyword.value.trim(), (status, result) => {
    const poi = result?.poiList?.pois?.[0]
    if (status !== 'complete' || !poi?.location) {
      showToast('没有找到匹配地点')
      return
    }
    selected.city = poi.cityname || selected.city
    selected.address = poi.address ? `${poi.name} ${poi.address}` : poi.name
    selected.longitude = Number(poi.location.lng.toFixed(6))
    selected.latitude = Number(poi.location.lat.toFixed(6))
    drawMarker([selected.longitude, selected.latitude])
  })
}

function confirm() {
  emit('select', { ...selected })
  close()
}

function close() {
  emit('update:show', false)
}
</script>
