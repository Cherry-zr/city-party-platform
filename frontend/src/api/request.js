import axios from 'axios'
import { showFailToast } from 'vant'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && payload.code !== 200) {
      showFailToast(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return payload.data
  },
  (error) => {
    showFailToast(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
