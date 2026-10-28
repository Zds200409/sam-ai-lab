import request from './request'

// ==================== 订单 ====================
export function getOrderPage(params) {
  return request({ url: '/orders/page', params })
}
export function getOrderById(id) {
  return request({ url: `/orders/${id}` })
}
export function checkout(data) {
  return request({ url: '/orders/checkout', method: 'post', data })
}
export function refundOrder(id) {
  return request({ url: `/orders/${id}/refund`, method: 'post' })
}
export function approveRefund(id) {
  return request({ url: `/orders/${id}/approve-refund`, method: 'post' })
}
export function rejectRefund(id) {
  return request({ url: `/orders/${id}/reject-refund`, method: 'post' })
}
