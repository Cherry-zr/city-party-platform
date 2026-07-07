import request from './request'

export function signupActivity(activityId, data = {}) {
  return request.post(`/api/activities/${activityId}/signup`, data)
}

export function cancelSignup(activityId) {
  return request.post(`/api/activities/${activityId}/signup/cancel`)
}

export function reviewSignup(signupId, status) {
  return request.post(`/api/signups/${signupId}/review`, { status })
}

export function mySignups(params) {
  return request.get('/api/signups/my', { params })
}
