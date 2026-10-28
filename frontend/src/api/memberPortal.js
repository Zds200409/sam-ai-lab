import axios from 'axios'
import { getMemberToken, clearMemberAuth } from '../utils/memberAuth'
import { ElMessage } from 'element-plus'

// 会员端独立请求实例，使用会员自己的 token，与后台 token 隔离
const memberRequest = axios.create({
  baseURL: '/api',
  timeout: 15000
})

memberRequest.interceptors.request.use(
  config => {
    const token = getMemberToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

memberRequest.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      clearMemberAuth()
      ElMessage.warning('请先登录会员账号')
      window.location.href = '/portal/login'
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 认证
export function register(data) {
  return memberRequest({ url: '/member-auth/register', method: 'post', data })
}
export function login(data) {
  return memberRequest({ url: '/member-auth/login', method: 'post', data })
}
export function getProfile() {
  return memberRequest({ url: '/member-auth/profile', method: 'get' })
}
export function updateProfile(data) {
  return memberRequest({ url: '/member-auth/profile', method: 'put', data })
}
export function changePassword(data) {
  return memberRequest({ url: '/member-auth/password', method: 'put', data })
}
export function getCaptcha() {
  return memberRequest({ url: '/auth/captcha', method: 'get' })
}
export function resetPassword(data) {
  return memberRequest({ url: '/member-auth/reset-password', method: 'post', data })
}
export function upgradeLevel(data) {
  return memberRequest({ url: '/member-auth/upgrade', method: 'post', data })
}

// 门户
export function getSummary() {
  return memberRequest({ url: '/portal/summary', method: 'get' })
}
export function getOrders(params) {
  return memberRequest({ url: '/portal/orders', method: 'get', params })
}
export function getPointsRecords(params) {
  return memberRequest({ url: '/portal/points-records', method: 'get', params })
}
export function getBenefits() {
  return memberRequest({ url: '/portal/benefits', method: 'get' })
}
export function redeem(benefitId) {
  return memberRequest({ url: `/portal/redeem/${benefitId}`, method: 'post' })
}

// 商品选购
export function getProducts(params) {
  return memberRequest({ url: '/portal/products', method: 'get', params })
}
export function placeOrder(data) {
  return memberRequest({ url: '/portal/order', method: 'post', data })
}

// 升级预览
export function getUpgradeInfo() {
  return memberRequest({ url: '/portal/upgrade-info', method: 'get' })
}

// 退换货（会员自助申请与查询）
export function applyReturn(data) {
  return memberRequest({ url: '/portal/return', method: 'post', data })
}
export function getMyReturns(params) {
  return memberRequest({ url: '/portal/returns', method: 'get', params })
}
