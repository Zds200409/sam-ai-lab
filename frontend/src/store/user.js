import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, getUser, setUser, setRefreshToken } from '../utils/auth'
import { login as loginApi, getCurrentUser } from '../api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: getUser() || null,
    menus: []
  }),
  actions: {
    async login(loginForm) {
      const res = await loginApi(loginForm)
      const { token, refreshToken, userId, username, realName, roles, menus } = res.data
      this.token = token
      setToken(token)
      if (refreshToken) setRefreshToken(refreshToken)
      this.userInfo = { userId, username, realName, roles }
      this.menus = menus || []
      setUser(this.userInfo)
      return res
    },
    async fetchCurrentUser() {
      const res = await getCurrentUser()
      const { userId, username, realName, roles, menus } = res.data
      this.userInfo = { userId, username, realName, roles }
      this.menus = menus || []
      setUser(this.userInfo)
    },
    logout() {
      this.token = ''
      this.userInfo = null
      this.menus = []
      removeToken()
    }
  }
})
