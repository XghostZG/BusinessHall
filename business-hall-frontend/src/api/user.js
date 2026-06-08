import api from '../utils/request'

export const userApi = {
  // 登录
  login: (data) => api.post('/user/login', data),
  
  // 退出登录
  logout: () => api.post('/user/logout'),
  
  // 注册
  register: (data) => api.post('/user/register', data),
  
  // 获取当前用户信息
  getCurrentUser: (userId) => api.get('/user/info', { params: { userId } }),
  
  // 更新用户信息（添加或更新）- 管理员使用
  save: (data) => api.post('/user/save', data),
  
  // 用户更新个人信息
  updateProfile: (data) => api.put('/user/update', data),
  
  // 修改密码
  changePassword: (userId, oldPassword, newPassword) => 
    api.put('/user/password', null, { params: { userId, oldPassword, newPassword } }),
  
  // 获取所有用户
  list: () => api.get('/user/list'),
  
  // 更新用户状态
  updateStatus: (userId, status) => api.put('/user/status', null, { params: { userId, status } }),
  
  // 获取用户总数
  getCount: () => api.get('/user/count'),
  
  // 新增营业员
  addClerk: (data) => api.post('/user/addClerk', data),
  
  // 更新用户角色
  updateRole: (userId, role) => api.put('/user/role', null, { params: { userId, role } }),

  // 删除用户
  delete: (userId) => api.delete('/user/delete', { params: { userId } })
}
