import request from './request'

// ==================== 权益管理 ====================
export function getBenefitList(levelId) {
  return request({ url: '/benefits/list', params: { levelId } })
}
export function createBenefit(data) {
  return request({ url: '/benefits', method: 'post', data })
}
export function updateBenefit(id, data) {
  return request({ url: `/benefits/${id}`, method: 'put', data })
}
export function deleteBenefit(id) {
  return request({ url: `/benefits/${id}`, method: 'delete' })
}

// ==================== 积分管理 ====================
export function adjustPoints(data) {
  return request({ url: '/points/adjust', method: 'post', data })
}
