import request from './request'

export function getAnnouncementPage(params) {
  return request({ url: '/announcements/page', params })
}
export function createAnnouncement(data) {
  return request({ url: '/announcements', method: 'post', data })
}
export function updateAnnouncement(id, data) {
  return request({ url: `/announcements/${id}`, method: 'put', data })
}
export function publishAnnouncement(id, status = 1) {
  // 后端 /announcements/{id}/publish 为"发布/撤回"两用接口，必须携带 status（1 发布、0 撤回）
  return request({ url: `/announcements/${id}/publish`, method: 'put', params: { status } })
}
export function unpublishAnnouncement(id) {
  return request({ url: `/announcements/${id}/publish`, method: 'put', params: { status: 0 } })
}
export function deleteAnnouncement(id) {
  return request({ url: `/announcements/${id}`, method: 'delete' })
}
