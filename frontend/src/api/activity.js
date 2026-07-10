import request from './request'

export function listActivities(params) {
  return request.get('/api/activities', { params })
}

export function listNearbyActivities(params) {
  return request.get('/api/activities/nearby', { params })
}

export function getActivity(id) {
  return request.get(`/api/activities/${id}`)
}

export function createActivity(data) {
  return request.post('/api/activities', data)
}

export function updateActivity(id, data) {
  return request.put(`/api/activities/${id}`, data)
}

export function cancelActivity(id) {
  return request.patch(`/api/activities/${id}/cancel`)
}

export function finishActivity(id) {
  return request.patch(`/api/activities/${id}/finish`)
}

export function myActivities(params) {
  return request.get('/api/activities/my', { params })
}

export function activitySignups(id, params) {
  return request.get(`/api/activities/${id}/signups`, { params })
}

export function joinWaitlist(id) {
  return request.post(`/api/activities/${id}/waitlist`)
}

export function cancelWaitlist(id) {
  return request.post(`/api/activities/${id}/waitlist/cancel`)
}

export function activityWaitlist(id, params) {
  return request.get(`/api/activities/${id}/waitlist`, { params })
}
