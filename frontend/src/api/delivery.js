import request from './request'

export function getDeliveryPage(params) {
  return request({ url: '/delivery/page', params })
}
export function createDelivery(orderId) {
  return request({ url: '/delivery/create', method: 'post', params: { orderId } })
}
export function pickupDelivery(id) {
  return request({ url: `/delivery/${id}/pickup`, method: 'put' })
}
export function completeDelivery(id) {
  return request({ url: `/delivery/${id}/complete`, method: 'put' })
}
