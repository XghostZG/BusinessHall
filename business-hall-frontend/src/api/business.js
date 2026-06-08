import api from '../utils/request'

export const businessApi = {
  // 获取所有业务类型
  list: () => api.get('/business-type/list'),

  // 创建业务类型
  create: (data) => api.post('/business-type/create', data),

  // 更新业务类型
  update: (data) => api.put('/business-type/update', data),

  // 删除业务类型
  delete: (id) => api.delete('/business-type/delete', { params: { id } })
}
