import request from './request'

export function myNotices(params) {
  return request.get('/api/notices/my', { params })
}

export function markNoticeRead(id) {
  return request.put(`/api/notices/${id}/read`)
}

export function markAllNoticesRead() {
  return request.put('/api/notices/read-all')
}

export function unreadNoticeCount() {
  return request.get('/api/notices/unread-count')
}
