<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 左侧：历史会话列表 -->
      <div class="history-panel" v-if="showHistory">
        <div class="panel-header">
          <h3>会话记录</h3>
          <el-button text @click="showHistory = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="history-list">
          <div v-if="historyLoading" class="loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
          <div v-else-if="historyList.length === 0" class="empty">
            暂无历史会话
          </div>
          <div
            v-else
            v-for="item in historyList"
            :key="item.sessionId"
            class="history-item"
            :class="{ active: item.sessionId === currentSessionId }"
            @click="loadSession(item)"
          >
            <div class="item-top">
              <span class="staff-name">{{ item.staffName || '待分配客服' }}</span>
              <el-tag :type="getStatusType(item.status)" size="small">
                {{ getStatusText(item.status) }}
              </el-tag>
            </div>
            <div class="item-preview">{{ item.lastMessage || '暂无消息' }}</div>
            <div class="item-time">{{ formatTime(item.lastMessageTime) }}</div>
          </div>
        </div>
      </div>

      <!-- 主聊天区域 -->
      <div class="chat-main">
        <!-- 连接状态栏 -->
        <div class="status-bar" v-if="!chatStore.isConnected">
          <el-button type="primary" @click="connectChat" :loading="connecting">
            连接到客服系统
          </el-button>
        </div>

        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="header-left">
            <h3>在线客服</h3>
            <el-tag v-if="chatStore.currentSession?.staffName" type="success" size="small">
              {{ chatStore.currentSession.staffName }}
            </el-tag>
            <el-tag v-else-if="chatStore.currentSession?.status === 'waiting'" type="warning" size="small">
              等待分配中...
            </el-tag>
          </div>
          <div class="header-right">
            <el-button text @click="showHistory = true" v-if="!showHistory">
              会话记录
            </el-button>
            <el-button text type="danger" @click="handleEndSession" v-if="chatStore.currentSession?.status === 'chatting'">
              结束会话
            </el-button>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="message-area" ref="messageArea">
          <div v-if="!chatStore.isConnected" class="not-connected">
            <el-empty description="请先连接客服系统" />
          </div>
          <div v-else-if="chatStore.messages.length === 0" class="empty-messages">
            <el-empty description="暂无消息，开始聊天吧" />
          </div>
          <div v-else class="message-list">
            <div
              v-for="msg in chatStore.messages"
              :key="msg.id"
              class="message-item"
              :class="getMessageClass(msg)"
            >
              <div class="message-content">
                <div class="message-header" v-if="msg.senderType !== 'system'">
                  <span class="sender-name">{{ msg.senderName }}</span>
                  <span class="message-time">{{ formatTime(msg.time) }}</span>
                </div>
                <div class="message-body" :class="{ system: msg.senderType === 'system' }">
                  {{ msg.content }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area" v-if="chatStore.currentSession?.status === 'chatting'">
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="请输入您的问题..."
              @keydown.enter.ctrl="sendMessage"
              maxlength="500"
              show-word-limit
            />
          </div>
          <div class="input-actions">
            <span class="hint">Ctrl + Enter 发送</span>
            <el-button type="primary" @click="sendMessage" :disabled="!inputMessage.trim()">
              发送
            </el-button>
          </div>
        </div>

        <!-- 等待区域 -->
        <div class="waiting-area" v-else-if="chatStore.currentSession?.status === 'waiting'">
          <div class="waiting-content">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            <p>正在为您分配客服，请稍候...</p>
            <el-button @click="cancelWait">取消等待</el-button>
          </div>
        </div>

        <!-- 结束区域 -->
        <div class="ended-area" v-else-if="chatStore.currentSession?.status === 'closed'">
          <div class="ended-content">
            <el-icon color="#67c23a" :size="48"><CircleCheck /></el-icon>
            <p>会话已结束</p>
            <el-button type="primary" @click="startNewSession">开始新会话</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Loading, CircleCheck } from '@element-plus/icons-vue'
import { chatApi } from '../api'
import { useChatStore } from '../store/chat'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const inputMessage = ref('')
const connecting = ref(false)
const messageArea = ref(null)
const showHistory = ref(false)
const historyList = ref([])
const historyLoading = ref(false)
const currentSessionId = ref(null)

let historyTimer = null
let messageTimer = null

// 保存回调引用，用于组件销毁时清除
let onAllocatedCallback = null
let onSessionEndedCallback = null

const scrollToBottom = () => {
  nextTick(() => {
    if (messageArea.value) {
      messageArea.value.scrollTop = messageArea.value.scrollHeight
    }
  })
}

watch(() => chatStore.messages.length, () => {
  scrollToBottom()
})

const connectChat = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  connecting.value = true
  try {
    const userId = userStore.user?.id
    const userName = userStore.user?.username
    
    if (!userId || !userName) {
      ElMessage.warning('用户信息不完整，请重新登录')
      router.push('/login')
      return
    }
    
    const session = await chatApi.getOrCreateSession(userId, userName)
    
    chatStore.setCurrentSession(session, userName, 'user')
    
    if (session?.sessionId) {
      await chatStore.loadHistory(session.sessionId)
      currentSessionId.value = session.sessionId
    }
    
    // 如果没有连接，就建立WebSocket连接
    // 无论会话状态如何，都需要WebSocket连接来接收消息
    if (!chatStore.isConnected) {
      chatStore.connect(userId, userName, 'user')
      
      // 设置回调并保存引用
      onAllocatedCallback = (data) => {
        ElMessage.success(data.content)
      }
      chatStore.setOnAllocated(onAllocatedCallback)
      
      onSessionEndedCallback = (data) => {
        ElMessage.info(data.content || '会话已结束')
      }
      chatStore.setOnSessionEnded(onSessionEndedCallback)
    }
    
  } catch (error) {
    ElMessage.error('连接失败，请重试')
  } finally {
    connecting.value = false
  }
}

const sendMessage = () => {
  if (!inputMessage.value.trim()) return
  
  const success = chatStore.sendMessage(inputMessage.value)
  if (success) {
    inputMessage.value = ''
  } else {
    ElMessage.error('发送失败，请检查网络')
  }
}

const handleEndSession = async () => {
  try {
    await ElMessageBox.confirm('确定要结束当前会话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    chatStore.endSession()
  } catch {}
}

const cancelWait = async () => {
  try {
    await ElMessageBox.confirm('确定要取消等待吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    chatStore.disconnect()
    chatStore.currentSession = null
    chatStore.messages = []
  } catch {}
}

const startNewSession = () => {
  chatStore.disconnect()
  chatStore.currentSession = null
  chatStore.messages = []
  connectChat()
}

const loadHistoryList = async () => {
  historyLoading.value = true
  try {
    const userId = userStore.user?.id
    if (!userId) return
    const list = await chatApi.getHistory(userId)
    historyList.value = list
  } catch (error) {
    console.error('加载历史失败', error)
  } finally {
    historyLoading.value = false
  }
}

const loadSession = async (item) => {
  showHistory.value = false
  chatStore.disconnect()
  chatStore.messages = []
  
  try {
    const messages = await chatApi.getMessages(item.sessionId)
    chatStore.messages = messages.map(msg => ({
      id: msg.id,
      senderType: msg.senderType,
      senderId: msg.senderId,
      senderName: msg.senderName,
      content: msg.content,
      time: msg.createTime,
      isRead: msg.isRead === 1
    }))
    currentSessionId.value = item.sessionId
    chatStore.currentSession = {
      sessionId: item.sessionId,
      staffName: item.staffName,
      status: item.status,
      userName: userStore.user?.username,
      role: 'user'
    }
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载会话失败')
  }
}

const getMessageClass = (msg) => {
  if (msg.senderType === 'system') return 'system-message'
  if (msg.senderType === 'user') return 'my-message'
  return 'other-message'
}

const getStatusType = (status) => {
  const types = { waiting: 'warning', chatting: 'success', closed: 'info' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { waiting: '等待中', chatting: '进行中', closed: '已结束' }
  return texts[status] || status
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' +
         date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const refreshMessages = async () => {
  const sessionId = chatStore.currentSession?.sessionId
  if (!sessionId) return
  
  try {
    const messages = await chatApi.getMessages(sessionId)
    const newMessages = messages.map(msg => ({
      id: msg.id,
      senderType: msg.senderType,
      senderId: msg.senderId,
      senderName: msg.senderName,
      content: msg.content,
      time: msg.createTime,
      isRead: msg.isRead === 1
    }))
    
    if (newMessages.length !== chatStore.messages.length) {
      chatStore.messages = newMessages
    }
  } catch (error) {
    console.error('刷新消息失败', error)
  }
}

const startRefreshTimers = () => {
  stopRefreshTimers()
  historyTimer = setInterval(loadHistoryList, 3000)
  messageTimer = setInterval(refreshMessages, 5000)
}

const stopRefreshTimers = () => {
  if (historyTimer) { clearInterval(historyTimer); historyTimer = null }
  if (messageTimer) { clearInterval(messageTimer); messageTimer = null }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    loadHistoryList()
    connectChat()
    startRefreshTimers()
  }
})

onUnmounted(() => {
  stopRefreshTimers()
  // 清除回调
  if (onAllocatedCallback) {
    chatStore.clearOnAllocated(onAllocatedCallback)
  }
  if (onSessionEndedCallback) {
    chatStore.clearOnSessionEnded(onSessionEndedCallback)
  }
  // 不在这里断开WebSocket连接，让用户可以在不同页面间切换而保持连接
  // 只有点击"结束会话"按钮时才断开连接
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 130px);
  min-height: 500px;
  background: #f5f7fa;
  padding: 20px;
}

.chat-container {
  display: flex;
  height: 100%;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 历史会话面板 */
.history-panel {
  width: 280px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.history-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 8px;
}

.history-item:hover {
  background: #f5f7fa;
}

.history-item.active {
  background: #ecf5ff;
}

.item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.staff-name {
  font-weight: 500;
  color: #333;
}

.item-preview {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.item-time {
  font-size: 11px;
  color: #c0c4cc;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

/* 主聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.status-bar {
  padding: 12px 16px;
  background: #fdf6ec;
  text-align: center;
  border-bottom: 1px solid #f5dab1;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  background: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h3 {
  margin: 0;
  font-size: 18px;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 消息区域 */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
}

.not-connected, .empty-messages {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  max-width: 70%;
}

.message-item.my-message {
  align-self: flex-end;
}

.message-item.other-message {
  align-self: flex-start;
}

.message-item.system-message {
  align-self: center;
  max-width: 100%;
}

.message-content {
  padding: 12px 16px;
  border-radius: 12px;
  word-break: break-word;
}

.my-message .message-content {
  background: #409eff;
  color: white;
  border-bottom-right-radius: 4px;
}

.other-message .message-content {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.system-message .message-content {
  background: #f4f4f5;
  color: #909399;
  font-size: 12px;
  text-align: center;
  border-radius: 20px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 12px;
}

.sender-name {
  font-weight: 500;
}

.message-time {
  opacity: 0.7;
  font-size: 11px;
}

.message-body {
  line-height: 1.6;
}

.message-body.system {
  font-style: italic;
}

/* 输入区域 */
.input-area {
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #eee;
}

.input-wrapper {
  margin-bottom: 12px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  font-size: 12px;
  color: #999;
}

/* 等待区域 */
.waiting-area, .ended-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.waiting-content, .ended-content {
  text-align: center;
}

.waiting-content p, .ended-content p {
  margin: 16px 0;
  color: #666;
  font-size: 16px;
}

.ended-content {
  color: #67c23a;
}
</style>
