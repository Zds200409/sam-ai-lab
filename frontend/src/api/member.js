import request from './request'

// ==================== 会员 CRUD ====================
export function getMemberPage(params) {
  return request({ url: '/members/page', params })
}
export function getMemberById(id) {
  return request({ url: `/members/${id}` })
}
export function createMember(data) {
  return request({ url: '/members', method: 'post', data })
}
export function updateMember(id, data) {
  return request({ url: `/members/${id}`, method: 'put', data })
}
export function freezeMember(id) {
  return request({ url: `/members/${id}/freeze`, method: 'post' })
}
export function unfreezeMember(id) {
  return request({ url: `/members/${id}/unfreeze`, method: 'post' })
}
export function renewMember(id) {
  return request({ url: `/members/${id}/renew`, method: 'post' })
}

// ==================== 会员详情 ====================
export function getMemberPointsRecords(memberId) {
  return request({ url: `/members/${memberId}/points-records` })
}

// ==================== 会员等级 ====================
export function getLevelList() {
  return request({ url: '/levels/list' })
}
export function getLevelPage(params) {
  return request({ url: '/levels/page', params })
}
export function createLevel(data) {
  return request({ url: '/levels', method: 'post', data })
}
export function updateLevel(id, data) {
  return request({ url: `/levels/${id}`, method: 'put', data })
}
