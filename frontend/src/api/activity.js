import request from './request'

export function listActivities(params) {
  return request.get('/api/activities', { params })
}

export function getActivity(id) {
  return request.get(`/api/activities/${id}`)
}

export function createActivity(data) {
  return request.post('/api/activities', data)
}

export function myActivities(params) {
  return request.get('/api/activities/my', { params })
}

export function activitySignups(id, params) {
  return request.get(`/api/activities/${id}/signups`, { params })
}
