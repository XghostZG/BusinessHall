import api from '../utils/request'

export const approvalApi = {
  // 提交申请（请假/投诉等统一入口）
  submit: (data) => api.post('/approval/submit', data),

  // 获取我的申请列表
  getMyList: (params) => api.get('/approval/my', { params }),

  // 获取审批申请列表（管理员）
  getList: (params) => api.get('/approval/list', { params }),

  // 获取待审批列表
  getPending: () => api.get('/approval/pending'),

  // 获取统计数据
  getStatistics: () => api.get('/approval/statistics'),

  // 审批申请
  approve: (id, result, remark) => api.post(`/approval/approve/${id}`, null, {
    params: { result, remark }
  }),

  // 获取申请详情
  getDetail: (id) => api.get(`/approval/detail/${id}`)
}
