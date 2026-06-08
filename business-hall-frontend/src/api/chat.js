import api from '../utils/request'

export const chatApi = {
  // 获取或创建会话
  getOrCreateSession: (userId, userName) => api.get('/chat/session', {
    params: { userId, userName }
  }),
  
  // 获取会话列表
  getSessionList: (staffId) => api.get('/chat/staff/list', {
    params: { staffId }
  }),
  
  // 获取会话消息
  getMessages: (sessionId) => api.get('/chat/messages', {
    params: { sessionId }
  }),
  
  // 获取历史会话
  getHistory: (userId) => api.get('/chat/history', {
    params: { userId }
  }),
  
  // 标记已读
  markAsRead: (sessionId, userId) => api.post('/chat/read', null, {
    params: { sessionId, userId }
  }),
  
  // 结束会话
  endSession: (sessionId, userId) => api.post('/chat/end', null, {
    params: { sessionId, userId }
  }),
  
  // 获取快捷回复列表
  getQuickReplies: () => api.get('/chat/quick-replies')
}
