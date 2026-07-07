import request from './request'

export function favoriteActivity(activityId) {
  return request.post(`/api/activities/${activityId}/favorite`)
}

export function unfavoriteActivity(activityId) {
  return request.delete(`/api/activities/${activityId}/favorite`)
}

export function myFavorites(params) {
  return request.get('/api/favorites/my', { params })
}
