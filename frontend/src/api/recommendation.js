import request from './request'

export function listRecommendedActivities(params) {
  return request.get('/api/recommendations/activities', { params })
}
