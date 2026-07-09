import axios from 'axios'
import { showFailToast } from 'vant'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

let redirectingToLogin = false

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
      if (Number(payload.code) === 401) {
        redirectToLogin()
      }
      showFailToast(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return payload.data
  },
  (error) => {
    if (error.response?.status === 401 || Number(error.response?.data?.code) === 401) {
      redirectToLogin()
    }
    showFailToast(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

function redirectToLogin() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (redirectingToLogin || window.location.pathname === '/login') {
    return
  }
  redirectingToLogin = true
  const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`
  window.location.replace(`/login?redirect=${encodeURIComponent(redirect)}`)
}

export default request
