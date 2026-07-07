import request from './request'

export function getCaptcha() {
  return request.get('/api/auth/captcha')
}

export function login(data) {
  return request.post('/api/auth/login', data)
}

export function register(data) {
  return request.post('/api/auth/register', data)
}
