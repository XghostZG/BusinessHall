import api from '../utils/request'

export const appointmentApi = {
  // 创建预约
  create: (data) => api.post('/appointment/create', data),
  
  // 取消预约
  cancel: (appointmentId) => api.post('/appointment/cancel', null, { params: { appointmentId } }),
  
  // 改签预约（使用日期和时段时间信息）
  reschedule: (appointmentId, newDate, newStartTime, newEndTime) => api.post('/appointment/reschedule', null, { 
    params: { 
      appointmentId, 
      newDate,
      newStartTime,
      newEndTime 
    } 
  }),
  
  // 获取用户预约列表
  getMyList: (userId) => api.get('/appointment/user', { params: { userId } }),
  
  // 获取今日预约（营业员，传入clerkId只返回自己负责的）
  getToday: (clerkId) => api.get('/appointment/today', { params: clerkId ? { clerkId } : {} }),
  
  // 获取所有预约列表（管理员）
  list: () => api.get('/appointment/list'),
  
  // 更新预约状态（仅用于取消/爽约）
  updateStatus: (appointmentId, status) => api.put('/appointment/status', null, {
    params: { appointmentId, status }
  }),
  
  // 核验码验证 + 开始办理
  startWithVerification: (appointmentId, clerkId, verificationCode) => api.post('/appointment/start', {
    appointmentId, clerkId, verificationCode
  }),
  
  // 输入处理结果 + 完成办理
  completeWithResult: (appointmentId, clerkId, result) => api.post('/appointment/complete', {
    appointmentId, clerkId, result
  }),
  
  // 统计 - 今日统计
  getTodayStatistics: () => api.get('/appointment/statistics/today'),
  
  // 统计 - 趋势
  getTrend: () => api.get('/appointment/statistics/trend'),
  
  // 统计 - 业务类型分布
  getBusinessTypeStats: () => api.get('/appointment/statistics/business-type'),
  
  // 统计 - 本周
  getWeekStats: () => api.get('/appointment/statistics/week'),
  
  // 统计 - 月度
  getMonthlyStats: () => api.get('/appointment/statistics/monthly'),
  
  // 统计 - 趋势（带对比）
  getTrends: () => api.get('/appointment/statistics/trends'),

  // 批量删除预约（管理员）
  batchDelete: (ids) => api.post('/appointment/batch-delete', ids),

  // 归档 - 预览可归档数量
  previewArchive: (params) => api.post('/appointment/archive/preview', params),

  // 归档 - 执行归档
  archiveAppointments: (params) => api.post('/appointment/archive', params),

  // 归档 - 获取归档记录列表
  getArchiveList: (params) => api.get('/appointment/archive', { params }),

  // 归档 - 恢复归档记录
  restoreArchive: (ids) => api.post('/appointment/archive/restore', ids)
}
