import request from './request'

export function dashboard() {
  return request.get('/api/admin/dashboard')
}

export function adminUsers(params) {
  return request.get('/api/admin/users', { params })
}

export function adminActivities(params) {
  return request.get('/api/admin/activities', { params })
}

export function adminSignups(params) {
  return request.get('/api/admin/signups', { params })
}

export function adminCredits(params) {
  return request.get('/api/admin/credits', { params })
}

export function adminReports(params) {
  return request.get('/api/admin/reports', { params })
}
