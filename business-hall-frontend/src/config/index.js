// 后端服务配置
export const config = {
  // 后端API基础URL
  apiBaseUrl: 'http://localhost:8080'
}

// 获取完整的图片URL
export const getImageUrl = (relativePath) => {
  if (!relativePath) return ''
  if (relativePath.startsWith('http://') || relativePath.startsWith('https://')) {
    return relativePath
  }
  if (relativePath.startsWith('/')) {
    return config.apiBaseUrl + relativePath
  }
  return config.apiBaseUrl + '/' + relativePath
}