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

export function createActivity(data, media) {
  if (media === undefined) {
    return request.post('/api/activities', data)
  }
  return request.post('/api/activities', buildActivityFormData(data, media))
}

export function updateActivity(id, data, media) {
  if (media === undefined) {
    return request.put(`/api/activities/${id}`, data)
  }
  return request.put(`/api/activities/${id}`, buildActivityFormData(data, media))
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

function buildActivityFormData(data, media = {}) {
  const form = new FormData()
  form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }), 'activity.json')
  if (media.coverFile) {
    form.append('cover', media.coverFile)
  }
  if (media.removeCover) {
    form.append('removeCover', 'true')
  }
  return form
}
