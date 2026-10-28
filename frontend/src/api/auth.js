import request from './request'

export function getCaptcha() {
  return request({ url: '/auth/captcha', method: 'get' })
}

export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

export function getCurrentUser() {
  return request({ url: '/auth/currentUser', method: 'get' })
}
export function changePassword(data) {
  return request({ url: '/auth/changePwd', method: 'put', data })
}
