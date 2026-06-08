import { defineStore } from 'pinia'
import { notificationApi } from '../api'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    unreadCount: 0,
    loading: false
  }),
  getters: {
    unreadNotifications: (state) => state.notifications.filter(n => !n.read),
    readNotifications: (state) => state.notifications.filter(n => n.read)
  },
  actions: {
    addNotification(notification) {
      const newNotification = {
        ...notification,
        read: notification.readStatus === 1
      }
      this.notifications.unshift(newNotification)
      if (!newNotification.read) {
        this.unreadCount++
      }
    },
    
    async markAsRead(notificationId) {
      try {
        await notificationApi.markAsRead(notificationId)
        const notification = this.notifications.find(n => n.id === notificationId)
        if (notification && !notification.read) {
          notification.read = true
          this.unreadCount--
        }
      } catch (error) {
        console.error('标记通知已读失败:', error)
      }
    },
    
    async markAllAsRead() {
      try {
        await notificationApi.markAllAsRead()
        this.notifications.forEach(n => { n.read = true })
        this.unreadCount = 0
      } catch (error) {
        console.error('标记所有通知已读失败:', error)
      }
    },
    
    async fetchNotifications() {
      this.loading = true
      try {
        const list = await notificationApi.list()
        this.notifications = list.map(n => ({
          ...n,
          read: n.readStatus === 1
        }))
        this.unreadCount = this.notifications.filter(n => !n.read).length
      } catch (error) {
        console.error('获取通知列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    async fetchUnreadCount() {
      try {
        const count = await notificationApi.getUnreadCount()
        this.unreadCount = count
      } catch (error) {
        console.error('获取未读通知数量失败:', error)
      }
    },
    
    clearAll() {
      this.notifications = []
      this.unreadCount = 0
    }
  }
})