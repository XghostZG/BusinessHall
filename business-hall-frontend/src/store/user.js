import { defineStore } from 'pinia'
import api from '../utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  }),
  getters: {
    isLoggedIn: (state) => {
      return !!(state.user && state.token)
    },
    userRole: (state) => state.user?.role || ''
  },
  actions: {
    setUser(user) {
      this.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    async logout() {
      try {
        await api.post('/user/logout')
      } catch (error) {
        console.error('退出登录请求失败:', error)
      }
      this.user = null
      this.token = ''
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user')
    },
    initUser() {
      const savedUser = localStorage.getItem('user') || sessionStorage.getItem('user')
      if (savedUser) {
        try {
          this.user = JSON.parse(savedUser)
        } catch (e) {
          console.error('解析用户信息失败:', e)
          this.logout()
        }
      }
    }
  }
})
