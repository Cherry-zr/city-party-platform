import request from './request'

export function getCaptcha() {
  return request.get('/api/auth/captcha')
}

export function getRegistrationCaptcha() {
  return request.get('/api/auth/register-captcha/challenge')
}

export function verifyRegistrationCaptcha(data) {
  return request.post('/api/auth/register-captcha/verify', data)
}

export function login(data) {
  return request.post('/api/auth/login', data)
}

export function register(data) {
  return request.post('/api/auth/register', data)
}
