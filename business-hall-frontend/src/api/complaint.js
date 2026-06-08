import api from '../utils/request'

export const complaintApi = {
  // 提交投诉
  create: (data) => api.post('/complaint/create', data),
  
  // 获取我的投诉列表
  getMyList: (userId) => api.get('/complaint/my', { params: { userId } }),
  
  // 获取投诉详情
  getDetail: (complaintId) => api.get('/complaint/detail', { params: { complaintId } }),
  
  // 处理投诉（营业员）
  handle: (complaintId, handleResult, handleRemark) =>
    api.post('/complaint/handle', {
      complaintId, handleResult, handleRemark
    }),
  
  // 审核投诉（管理员）
  review: (complaintId, finalDecision, feedbackToUser, decisionBy) =>
    api.post('/complaint/decision', {
      complaintId, finalDecision, feedbackToUser, decisionBy
    }),
  
  // 获取所有投诉（管理员）
  list: () => api.get('/complaint/list'),
  
  // 获取待处理投诉（营业员）
  getPendingForStaff: () => api.get('/complaint/pending/staff'),
  
  // 获取待审核投诉（管理员）
  getPendingForAdmin: () => api.get('/complaint/pending/admin'),
  
  // 获取投诉统计
  getStatistics: () => api.get('/complaint/statistics')
}
