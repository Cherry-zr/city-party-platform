import { defineStore } from 'pinia'
import { markAllNoticesRead, markNoticeRead, myNotices, unreadNoticeCount } from '../api/notice'

export const useNoticeStore = defineStore('notice', {
  state: () => ({
    unreadCount: 0,
    items: [],
    loading: false,
    error: ''
  }),
  actions: {
    async loadUnreadCount() {
      this.unreadCount = await unreadNoticeCount()
    },
    async loadNotices(params = { current: 1, size: 50 }) {
      this.loading = true
      this.error = ''
      try {
        const data = await myNotices(params)
        this.items = data.records || []
        await this.loadUnreadCount()
        return data
      } catch (error) {
        this.error = error.message || '通知加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async markRead(item) {
      if (!item || item.read) return
      await markNoticeRead(item.id)
      item.read = true
      if (this.unreadCount > 0) {
        this.unreadCount -= 1
      }
    },
    async markAllRead() {
      await markAllNoticesRead()
      this.items = this.items.map((item) => ({ ...item, read: true }))
      this.unreadCount = 0
    },
    receiveRealtimeNotice(payload) {
      const notice = {
        id: payload.noticeId,
        type: payload.noticeType,
        title: payload.title,
        content: payload.content,
        relatedId: payload.relatedId,
        createdAt: payload.createdAt,
        read: false
      }
      if (!this.items.some((item) => item.id === notice.id)) {
        this.items.unshift(notice)
        this.unreadCount += 1
      }
    }
  }
})
