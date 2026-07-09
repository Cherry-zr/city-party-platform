import request from './request'

export function getReviewTargets(activityId) {
  return request.get(`/api/activities/${activityId}/reviews/targets`)
}

export function submitActivityReview(activityId, data) {
  return request.post(`/api/activities/${activityId}/reviews`, data)
}

export function getActivityReviews(activityId, params) {
  return request.get(`/api/activities/${activityId}/reviews`, { params })
}

export function getMyReviews(params) {
  return request.get('/api/reviews/my', { params })
}
