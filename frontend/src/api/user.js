import request from './request'

export function getMe() {
  return request.get('/api/user/me')
}

export function updateProfile(data) {
  return request.put('/api/user/profile', data)
}

export function getPublicProfile(id) {
  return request.get(`/api/users/${id}/public-profile`)
}

export function uploadAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/file/upload/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadActivityCover(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/file/upload/activity-cover', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
