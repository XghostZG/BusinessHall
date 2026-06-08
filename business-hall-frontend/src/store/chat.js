import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatApi } from '../api'

export const useChatStore = defineStore('chat', () => {
  // WebSocket 连接状态
  const isConnected = ref(false)
  const ws = ref(null)
  
  // 当前会话信息
  const currentSession = ref(null)
  const messages = ref([])
  const unreadCount = ref(0)
  
  // 连接参数（用于重连）
  let connectionParams = null
  
  // 回调函数（支持多个回调）
  let onMessageCallbacks = []
  let onSystemCallbacks = []
  let onAllocatedCallbacks = []
  let onSessionEndedCallbacks = []
  
  // 初始化 WebSocket 连接
  const connect = (userId, userName, role) => {
    // 保存连接参数
    connectionParams = { userId, userName, role }
    
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      console.log('WebSocket 已连接')
      return
    }
    
    // 如果已有连接中的实例，先关闭
    if (ws.value && ws.value.readyState === WebSocket.CONNECTING) {
      console.log('WebSocket 正在连接中，先关闭')
      ws.value.close()
      return
    }
    
    // 使用相对路径，由 Vite 代理转发到后端
    const wsUrl = `/chat/ws?userId=${userId}&userName=${encodeURIComponent(userName)}&role=${role}`
    console.log('连接 WebSocket:', wsUrl)
    
    try {
      ws.value = new WebSocket(wsUrl)
    } catch (err) {
      console.error('创建 WebSocket 失败:', err)
      return
    }
    
    ws.value.onopen = () => {
      console.log('WebSocket 连接成功')
      isConnected.value = true
      startHeartbeat()
    }
    
    ws.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        handleMessage(data)
      } catch (e) {
        console.error('解析消息失败:', e)
      }
    }
    
    ws.value.onerror = (error) => {
      console.error('WebSocket 错误:', error)
      isConnected.value = false
    }
    
    ws.value.onclose = (event) => {
      console.log('WebSocket 连接关闭, code:', event.code, 'reason:', event.reason)
      isConnected.value = false
      stopHeartbeat()
      
      // 如果不是手动断开(1000)，且有连接参数，自动重连
      if (event.code !== 1000 && connectionParams) {
        console.log('准备自动重连...')
        // 增加重连间隔时间，避免频繁重连
        setTimeout(() => {
          if (connectionParams) {
            console.log('执行自动重连')
            connect(connectionParams.userId, connectionParams.userName, connectionParams.role)
          }
        }, 5000)
      }
    }
  }
  
  // 处理收到的消息
  const handleMessage = (data) => {
    console.log('收到消息:', data)
    
    switch (data.type) {
      case 'chat':
        // 普通聊天消息
        messages.value.push({
          id: data.messageId,
          senderType: data.senderType,
          senderId: data.senderId,
          senderName: data.senderName,
          content: data.content,
          time: data.time,
          isRead: true
        })
        onMessageCallbacks.forEach(callback => callback(data))
        break
        
      case 'system':
        // 系统消息
        messages.value.push({
          id: data.messageId,
          senderType: 'system',
          senderName: '系统',
          content: data.content,
          time: data.time,
          isRead: true
        })
        onSystemCallbacks.forEach(callback => callback(data))
        break
        
      case 'allocated':
        // 已分配客服 - 更新会话信息，保留 userName 和 role
        currentSession.value = {
          ...currentSession.value,
          sessionId: data.sessionId,
          staffName: data.staffName,
          status: 'chatting'
        }
        onAllocatedCallbacks.forEach(callback => callback(data))
        break
        
      case 'new_customer':
        // 新客户接入通知（营业员侧）
        onAllocatedCallbacks.forEach(callback => callback(data))
        break

      case 'waiting':
        // 等待中
        currentSession.value = {
          sessionId: data.sessionId,
          status: 'waiting'
        }
        onSystemCallbacks.forEach(callback => callback(data))
        break
        
      case 'session_ended':
      case 'session_closed':
        // 会话结束
        currentSession.value = {
          ...currentSession.value,
          status: 'closed'
        }
        onSessionEndedCallbacks.forEach(callback => callback(data))
        break
        
      case 'read_ack':
        // 已读确认
        break
        
      case 'pong':
        // 心跳响应
        break
    }
  }
  
  // 发送聊天消息
  const sendMessage = (content) => {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      console.error('WebSocket 未连接')
      return false
    }
    
    if (!currentSession.value || !currentSession.value.sessionId) {
      console.error('无活动会话')
      return false
    }
    
    const data = {
      type: 'chat',
      sessionId: currentSession.value.sessionId,
      content: content,
      senderType: currentSession.value.role || 'user',
      senderName: currentSession.value.userName || ''
    }
    
    ws.value.send(JSON.stringify(data))
    return true
  }
  
  // 发送已读标记
  const sendRead = () => {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      return
    }
    
    if (!currentSession.value || !currentSession.value.sessionId) {
      return
    }
    
    const data = {
      type: 'read',
      sessionId: currentSession.value.sessionId
    }
    
    ws.value.send(JSON.stringify(data))
  }
  
  // 结束会话
  const endSession = () => {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      return
    }
    
    const data = {
      type: 'end_session'
    }
    
    ws.value.send(JSON.stringify(data))
  }
  
  // 心跳
  let heartbeatTimer = null
  
  const startHeartbeat = () => {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (ws.value && ws.value.readyState === WebSocket.OPEN) {
        ws.value.send(JSON.stringify({ type: 'ping' }))
      }
    }, 30000) // 30秒心跳
  }
  
  const stopHeartbeat = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }
  
  // 断开连接
  const disconnect = () => {
    connectionParams = null  // 清除连接参数，防止自动重连
    stopHeartbeat()
    if (ws.value) {
      ws.value.close(1000, '手动断开')
      ws.value = null
    }
    isConnected.value = false
    currentSession.value = null
    messages.value = []
    // 不清除回调，回调由注册它们的组件自己在销毁时清除
    // 这样可以避免用户端断开连接影响客服端的回调
  }
  
  // 设置回调（添加到回调数组）
  const setOnMessage = (callback) => {
    if (callback && !onMessageCallbacks.includes(callback)) {
      onMessageCallbacks.push(callback)
    }
  }
  
  const setOnSystem = (callback) => {
    if (callback && !onSystemCallbacks.includes(callback)) {
      onSystemCallbacks.push(callback)
    }
  }
  
  const setOnAllocated = (callback) => {
    if (callback && !onAllocatedCallbacks.includes(callback)) {
      onAllocatedCallbacks.push(callback)
    }
  }
  
  const setOnSessionEnded = (callback) => {
    if (callback && !onSessionEndedCallbacks.includes(callback)) {
      onSessionEndedCallbacks.push(callback)
    }
  }
  
  // 清除特定回调
  const clearOnMessage = (callback) => {
    onMessageCallbacks = onMessageCallbacks.filter(cb => cb !== callback)
  }
  
  const clearOnSystem = (callback) => {
    onSystemCallbacks = onSystemCallbacks.filter(cb => cb !== callback)
  }
  
  const clearOnAllocated = (callback) => {
    onAllocatedCallbacks = onAllocatedCallbacks.filter(cb => cb !== callback)
  }
  
  const clearOnSessionEnded = (callback) => {
    onSessionEndedCallbacks = onSessionEndedCallbacks.filter(cb => cb !== callback)
  }
  
  // 设置当前会话信息
  const setCurrentSession = (session, userName, role) => {
    currentSession.value = {
      ...session,
      userName,
      role
    }
  }
  
  // 加载历史消息
  const loadHistory = async (sessionId) => {
    try {
      const msgList = await chatApi.getMessages(sessionId)
      messages.value = msgList.map(msg => ({
        id: msg.id,
        senderType: msg.senderType,
        senderId: msg.senderId,
        senderName: msg.senderName,
        content: msg.content,
        time: msg.createTime,
        isRead: msg.isRead === 1
      }))
    } catch (error) {
      console.error('加载历史消息失败:', error)
    }
  }
  
  return {
    isConnected,
    currentSession,
    messages,
    unreadCount,
    connect,
    disconnect,
    sendMessage,
    sendRead,
    endSession,
    setOnMessage,
    setOnSystem,
    setOnAllocated,
    setOnSessionEnded,
    clearOnMessage,
    clearOnSystem,
    clearOnAllocated,
    clearOnSessionEnded,
    setCurrentSession,
    loadHistory
  }
})
