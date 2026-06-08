import api from '../utils/request'

export const leaveApi = {
  // 获取我的请假记录
  getMy: () => api.get('/leave/my'),

  // 申请请假
  apply: (data) => api.post('/leave/apply', data),

  // 获取待审批请假（管理员/营业员）
  getPending: () => api.get('/leave/pending'),

  // 获取所有请假记录（管理员）
  getAll: () => api.get('/leave/all'),

  // 审批请假
  approve: (id, data) => api.put(`/leave/approve/${id}`, data)
}
