import axios from 'axios'
import { getToken, removeToken, setToken, getRefreshToken, setRefreshToken } from '../utils/auth'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 防止并发刷新
let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(newToken) {
  refreshSubscribers.forEach(cb => cb(newToken))
  refreshSubscribers = []
}

function addRefreshSubscriber(cb) {
  refreshSubscribers.push(cb)
}

request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  async error => {
    const originalRequest = error.config

    if (error.response && (error.response.status === 401 || error.response.status === 403) && !originalRequest._retry) {
      const refreshToken = getRefreshToken()
      if (refreshToken) {
        if (isRefreshing) {
          // 正在刷新中，将请求加入队列
          return new Promise(resolve => {
            addRefreshSubscriber(newToken => {
              originalRequest.headers.Authorization = `Bearer ${newToken}`
              originalRequest._retry = true
              resolve(request(originalRequest))
            })
          })
        }

        originalRequest._retry = true
        isRefreshing = true

        try {
          const res = await axios.post('/api/auth/refresh', { refreshToken })
          if (res.data.code === 200) {
            const { token, refreshToken: newRefreshToken } = res.data.data
            setToken(token)
            if (newRefreshToken) setRefreshToken(newRefreshToken)
            isRefreshing = false
            onRefreshed(token)
            originalRequest.headers.Authorization = `Bearer ${token}`
            return request(originalRequest)
          }
        } catch (e) {
          // refresh 失败，清除状态并跳转登录
        }

        isRefreshing = false
        refreshSubscribers = []
      }

      // 无 refresh token 或刷新失败 → 跳转登录
      removeToken()
      ElMessage.warning('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(error)
    }

    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
