let amapPromise = null

export function getAmapConfig() {
  return {
    key: import.meta.env.VITE_AMAP_KEY || '',
    securityCode: import.meta.env.VITE_AMAP_SECURITY_CODE || ''
  }
}

export function hasAmapConfig() {
  const config = getAmapConfig()
  return Boolean(config.key && config.securityCode)
}

export function loadAmap() {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (amapPromise) {
    return amapPromise
  }
  const config = getAmapConfig()
  if (!config.key || !config.securityCode) {
    return Promise.reject(new Error('高德地图 Key 或安全密钥未配置'))
  }
  window._AMapSecurityConfig = {
    securityJsCode: config.securityCode
  }
  amapPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(config.key)}&plugin=AMap.Geolocation,AMap.PlaceSearch,AMap.Geocoder`
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => reject(new Error('高德地图加载失败，请检查 Key、安全密钥或网络'))
    document.head.appendChild(script)
  })
  return amapPromise
}

export const cityCenters = {
  北京: [116.397428, 39.90923],
  上海: [121.473667, 31.230525],
  广州: [113.264385, 23.129112],
  深圳: [114.057868, 22.543099],
  杭州: [120.15507, 30.274085]
}

export function getCityCenter(city) {
  return cityCenters[city] || cityCenters.北京
}
