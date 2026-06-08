import api from '../utils/request'

export const notificationApi = {
  // 获取通知列表
  list: () => api.get('/notification/list'),
  
  // 标记通知为已读
  markAsRead: (notificationId) => api.put('/notification/read', null, { params: { notificationId } }),
  
  // 标记所有通知为已读
  markAllAsRead: () => api.put('/notification/read-all'),
  
  // 删除通知
  delete: (notificationId) => api.delete(`/notification/${notificationId}`),
  
  // 获取未读通知数
  getUnreadCount: () => api.get('/notification/unread-count')
}
