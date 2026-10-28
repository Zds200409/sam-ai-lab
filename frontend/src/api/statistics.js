import request from './request'

// ==================== 数据统计 ====================
export function getDashboard() {
  return request({ url: '/statistics/dashboard' })
}
export function getSalesTrend(days = 30) {
  return request({ url: '/statistics/sales-trend', params: { days } })
}
export function getSalesByCategory() {
  return request({ url: '/statistics/sales-by-category' })
}
export function getMemberAnalysis() {
  return request({ url: '/statistics/member-analysis' })
}
export function getTopProducts(limit = 10) {
  return request({ url: '/statistics/top-products', params: { limit } })
}
