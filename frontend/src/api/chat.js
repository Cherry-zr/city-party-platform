import request from './request'

export function checkChatAccess(activityId) {
  return request.get(`/api/activities/${activityId}/chat/access`)
}

export function getChatMessages(activityId, params) {
  return request.get(`/api/activities/${activityId}/chat/messages`, { params })
}

export function sendChatMessage(activityId, data) {
  return request.post(`/api/activities/${activityId}/chat/messages`, data)
}
