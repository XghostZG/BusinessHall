import api from '../utils/request'

export const consultationApi = {
  // 提交咨询
  create: (data) => api.post('/consultation/create', data),
  
  // 获取我的咨询列表
  getMyList: (userId) => api.get('/consultation/my', { params: { userId } }),
  
  // 获取咨询详情
  getDetail: (consultationId) => api.get('/consultation/detail', { params: { consultationId } }),
  
  // 回复咨询（营业员）
  reply: (consultationId, replyContent, handlerId) => 
    api.post('/consultation/reply', null, { 
      params: { consultationId, replyContent, handlerId } 
    }),
  
  // 获取待处理咨询（营业员）
  getPending: () => api.get('/consultation/pending'),
  
  // 获取所有咨询（管理员）
  list: () => api.get('/consultation/list')
}
