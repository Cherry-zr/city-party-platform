import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi } from '../api/auth'
import { getMe } from '../api/user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN'
  },
  actions: {
    saveSession(payload) {
      this.token = payload.token
      this.user = payload.user
      localStorage.setItem('token', payload.token)
      localStorage.setItem('user', JSON.stringify(payload.user))
    },
    async login(form) {
      const data = await loginApi(form)
      this.saveSession(data)
    },
    async register(form) {
      const data = await registerApi(form)
      this.saveSession(data)
    },
    async refreshMe() {
      if (!this.token) return
      this.user = await getMe()
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
