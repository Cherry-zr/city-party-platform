import request from './request'

export function getMe() {
  return request.get('/api/user/me')
}

export function getProfileOverview() {
  return request.get('/api/user/profile-overview')
}

export function updateProfile(data, media) {
  if (media === undefined) {
    return request.put('/api/user/profile', data)
  }
  const form = new FormData()
  form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }), 'profile.json')
  if (media.avatarFile) {
    form.append('avatar', media.avatarFile)
  }
  if (media.removeAvatar) {
    form.append('removeAvatar', 'true')
  }
  return request.put('/api/user/profile', form)
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
