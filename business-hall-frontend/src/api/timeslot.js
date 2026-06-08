import api from '../utils/request'

export const timeslotApi = {
  // 获取可用时间槽
  getAvailable: (date) => api.get('/time-slot/available', { 
    params: { date } 
  }),
  
  // 生成时间槽
  generate: () => api.post('/time-slot/generate'),
  
  // 获取时间槽列表
  list: () => api.get('/time-slot/list'),
  
  // 更新时间槽状态
  updateStatus: (timeSlotId, isActive) => api.post('/time-slot/update-status', null, {
    params: { timeSlotId, isActive }
  }),
  
  // 更新过期时间槽
  updateExpired: () => api.post('/time-slot/update-expired'),
  
  // 删除时间槽
  delete: (timeSlotId) => api.delete('/time-slot/delete', { params: { timeSlotId } }),
  
  // 获取时间槽模板列表
  getTemplate: () => api.get('/time-slot-template/list'),
  
  // 创建时间槽模板
  createTemplate: (data) => api.post('/time-slot-template/create', data),
  
  // 更新时间槽模板
  updateTemplate: (data) => api.put('/time-slot-template/update', data),
  
  // 删除时间槽模板
  deleteTemplate: (id) => api.delete('/time-slot-template/delete', { params: { id } }),
  
  // 根据条件获取模板
  getTemplatesByConditions: (weekday, businessTypeId) => 
    api.get('/time-slot-template/list-by-conditions', { 
      params: { weekday, businessTypeId } 
    }),
  
  // 获取模板详情
  getTemplateById: (id) => api.get('/time-slot-template/get', { params: { id } })
}
