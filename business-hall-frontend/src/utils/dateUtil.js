// 日期工具函数

/**
 * 格式化日期为 YYYY-MM-DD 格式（使用本地时间）
 * @param {Date|string} date - 日期对象或日期字符串
 * @returns {string} 格式化后的日期字符串
 */
export const formatDate = (date) => {
  if (!date) return ''
  
  const dateObj = typeof date === 'string' ? new Date(date) : date
  const year = dateObj.getFullYear()
  const month = String(dateObj.getMonth() + 1).padStart(2, '0')
  const day = String(dateObj.getDate()).padStart(2, '0')
  
  return `${year}-${month}-${day}`
}

/**
 * 禁用过去的日期（用于日期选择器）
 * @param {Date} time - 日期对象
 * @returns {boolean} 是否禁用
 */
export const disabledDate = (time) => {
  return time.getTime() < new Date().setHours(0, 0, 0, 0)
}
