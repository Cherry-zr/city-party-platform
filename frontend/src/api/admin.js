import request from './request'

export function dashboard() {
  return request.get('/api/admin/dashboard')
}

export function adminUsers(params) {
  return request.get('/api/admin/users', { params })
}

export function adminUserDetail(id) {
  return request.get(`/api/admin/users/${id}`)
}

export function adminActivities(params) {
  return request.get('/api/admin/activities', { params })
}

export function adminActivityDetail(id) {
  return request.get(`/api/admin/activities/${id}`)
}

export function adminActivitySignups(id, params) {
  return request.get(`/api/admin/activities/${id}/signups`, { params })
}

export function adminActivityWaitlist(id, params) {
  return request.get(`/api/admin/activities/${id}/waitlist`, { params })
}

export function adminSignups(params) {
  return request.get('/api/admin/signups', { params })
}

export function adminReviews(params) {
  return request.get('/api/admin/reviews', { params })
}

export function adminCredits(params) {
  return request.get('/api/admin/credits', { params })
}

export function adminNotices(params) {
  return request.get('/api/admin/notices', { params })
}

export function adminReports(params) {
  return request.get('/api/admin/reports', { params })
}
