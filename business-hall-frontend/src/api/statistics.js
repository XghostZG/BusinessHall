import api from '../utils/request'

export const statisticsApi = {
  // 今日预约统计
  getTodayStats: () => api.get('/appointment/statistics/today'),
  
  // 近7天预约趋势
  getTrend: () => api.get('/appointment/statistics/trend'),
  
  // 业务类型统计
  getBusinessTypeStats: () => api.get('/appointment/statistics/business-type'),
  
  // 月度预约统计
  getMonthlyStats: () => api.get('/appointment/statistics/monthly'),
  
  // 趋势统计（同比）
  getTrends: () => api.get('/appointment/statistics/trends'),
  
  // 本周预约统计
  getWeekStats: () => api.get('/appointment/statistics/week')
}
