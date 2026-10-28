import request from './request'

export function getOperationLogPage(params) {
  return request({ url: '/operation-logs/page', params })
}
