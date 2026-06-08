import api from '../utils/request'

export const resourceApi = {
  // 获取所有预约资源配置
  list: () => api.get('/resource/list'),
  
  // 获取工作日配置
  getWorkday: () => api.get('/resource/workday'),
  
  // 根据星期获取配置
  getByDay: (dayOfWeek) => api.get(`/resource/day/${dayOfWeek}`),
  
  // 更新资源配置
  update: (data) => api.put('/resource/update', data),
  
  // 获取指定日期的可预约信息
  getByDate: (date) => api.get(`/resource/date/${date}`),
  
  // 获取所有可办理业务类型
  getBusinessTypes: () => api.get('/resource/business-types'),
  
  // 切换资源配置状态
  toggleStatus: (id) => api.put(`/resource/toggle/${id}`)
}
