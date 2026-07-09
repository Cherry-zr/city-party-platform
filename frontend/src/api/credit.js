import request from './request'

export function getCreditLogs(params) {
  return request.get('/api/credit/logs', { params })
}
