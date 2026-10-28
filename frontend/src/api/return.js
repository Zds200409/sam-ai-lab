import request from './request'

export function getReturnPage(params) {
  return request({ url: '/returns/page', params })
}
export function createReturn(data) {
  return request({ url: '/returns', method: 'post', data })
}
export function approveReturn(id) {
  return request({ url: `/returns/${id}/approve`, method: 'put' })
}
export function rejectReturn(id) {
  return request({ url: `/returns/${id}/reject`, method: 'put' })
}
