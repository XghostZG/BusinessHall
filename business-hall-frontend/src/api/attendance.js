import api from '../utils/request'

export const attendanceApi = {
  // 获取我的考勤记录
  getMy: (month) => api.get('/attendance/my', { params: { month } }),

  // 获取考勤统计
  getStats: (month) => api.get('/attendance/stats', { params: { month } }),

  // 获取今日考勤
  getToday: () => api.get('/attendance/today'),

  // 打卡（签到/签退）
  checkin: (type) => api.post('/attendance/checkin', null, { params: { type } }),

  // 获取所有考勤记录（管理员）
  getAll: (month) => api.get('/attendance/all', { params: { month } })
}
