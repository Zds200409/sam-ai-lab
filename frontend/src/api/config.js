import request from './request'

export function getConfigList(params) {
  return request({ url: '/configs/list', params })
}
export function getConfigValue(configKey) {
  return request({ url: `/configs/value/${configKey}` })
}
export function updateConfig(id, data) {
  return request({ url: `/configs/${id}`, method: 'put', data })
}
