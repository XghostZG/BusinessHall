<template>
    <div class="chat-workbench">
        <div class="workbench-container">
            <!-- 左侧：客户列表 -->
            <div class="customer-list">
                <div class="list-header">
                    <h3>客户列表</h3>
                    <el-badge :value="totalUnread" :hidden="totalUnread === 0" type="primary">
                        <span class="unread-text">未读</span>
                    </el-badge>
                </div>

                <!-- 状态统计 -->
                <div class="status-stats">
                    <div class="stat-item waiting">
                        <span class="stat-num">{{ waitingCount }}</span>
                        <span class="stat-label">等待中</span>
                    </div>
                    <div class="stat-item chatting">
                        <span class="stat-num">{{ chattingCount }}</span>
                        <span class="stat-label">进行中</span>
                    </div>
                    <div class="stat-item closed">
                        <span class="stat-num">{{ closedCount }}</span>
                        <span class="stat-label">已完成</span>
                    </div>
                </div>

                <!-- 客户列表 -->
                <div class="customer-tabs">
                    <el-tabs v-model="activeTab">
                        <el-tab-pane label="进行中" name="chatting">
                            <div v-if="chattingList.length === 0" class="empty-list">
                                暂无进行中的会话
                            </div>
                            <div v-else v-for="item in chattingList" :key="item.sessionId" class="customer-item"
                                :class="{ active: currentSessionId === item.sessionId, unread: item.unreadCount > 0 }"
                                @click="selectCustomer(item)">
                                <div class="item-avatar">
                                    <span>{{ getAvatarText(item.userName) }}</span>
                                </div>
                                <div class="item-info">
                                    <div class="item-top">
                                        <span class="customer-name">{{ item.userName }}</span>
                                        <span class="item-time">{{ formatTime(item.lastMessageTime) }}</span>
                                    </div>
                                    <div class="item-preview">{{ item.lastMessage || '暂无消息' }}</div>
                                </div>
                                <div class="unread-badge" v-if="item.unreadCount > 0">
                                    {{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
                                </div>
                            </div>
                        </el-tab-pane>

                        <el-tab-pane :label="'等待中(' + waitingList.length + ')'" name="waiting">
                            <div v-if="waitingList.length === 0" class="empty-list">
                                暂无等待中的客户
                            </div>
                            <div v-else v-for="item in waitingList" :key="item.sessionId"
                                class="customer-item waiting-item" @click="acceptCustomer(item)">
                                <div class="item-avatar">
                                    <span>{{ getAvatarText(item.userName) }}</span>
                                </div>
                                <div class="item-info">
                                    <div class="item-top">
                                        <span class="customer-name">{{ item.userName }}</span>
                                        <el-tag type="warning" size="small">等待</el-tag>
                                    </div>
                                    <div class="item-preview">等待分配中...</div>
                                </div>
                                <el-button type="primary" size="small" circle>
                                    <el-icon>
                                        <Phone />
                                    </el-icon>
                                </el-button>
                            </div>
                        </el-tab-pane>
                    </el-tabs>
                </div>
            </div>

            <!-- 右侧：聊天窗口 -->
            <div class="chat-window">
                <template v-if="currentSession">
                    <!-- 聊天头部 -->
                    <div class="window-header">
                        <div class="header-info">
                            <span class="customer-name">{{ currentSession.userName }}</span>
                            <el-tag :type="getStatusType(currentSession.status)" size="small">
                                {{ getStatusText(currentSession.status) }}
                            </el-tag>
                        </div>
                        <div class="header-actions">
                            <el-button text type="danger" @click="closeSession">
                                结束会话
                            </el-button>
                        </div>
                    </div>

                    <!-- 消息区域 -->
                    <div class="window-messages" ref="messageArea">
                        <div v-if="messages.length === 0" class="empty-messages">
                            <el-empty description="暂无消息" />
                        </div>
                        <div v-else class="message-list">
                            <div v-for="msg in messages" :key="msg.id" class="message-item"
                                :class="getMessageClass(msg)">
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

                    <!-- 快捷回复 -->
                    <div class="quick-replies" v-if="quickReplies.length > 0">
                        <span class="quick-label">快捷回复：</span>
                        <el-tag v-for="reply in quickReplies" :key="reply.id" class="quick-item"
                            @click="useQuickReply(reply.content)">
                            {{ reply.content }}
                        </el-tag>
                    </div>

                    <!-- 输入区域 -->
                    <div class="window-input">
                        <el-input v-model="inputMessage" type="textarea" :rows="3" placeholder="输入回复内容..."
                            @keydown.enter.ctrl="sendMessage" maxlength="500" show-word-limit />
                        <div class="input-actions">
                            <span class="hint">Ctrl + Enter 发送</span>
                            <el-button type="primary" @click="sendMessage" :disabled="!inputMessage.trim()">
                                发送
                            </el-button>
                        </div>
                    </div>
                </template>

                <template v-else>
                    <div class="no-selection">
                        <el-empty description="请从左侧选择一个客户会话" />
                    </div>
                </template>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Phone } from '@element-plus/icons-vue'
import { chatApi } from '../../api'
import { useChatStore } from '../../store/chat'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const chatStore = useChatStore()

const activeTab = ref('chatting')

// 保存回调引用，用于组件销毁时清除
let onAllocatedCallback = null
let onSystemCallback = null
const currentSessionId = ref(null)
const currentSession = ref(null)
const messages = ref([])
const inputMessage = ref('')
const quickReplies = ref([])
const messageArea = ref(null)

// 统计
const totalUnread = computed(() => {
    return chattingList.value.reduce((sum, item) => sum + (item.unreadCount || 0), 0)
})

const chattingList = ref([])
const waitingList = ref([])
const closedList = ref([])

const chattingCount = computed(() => chattingList.value.length)
const waitingCount = computed(() => waitingList.value.length)
const closedCount = computed(() => closedList.value.length)

// 滚动到底部
const scrollToBottom = () => {
    nextTick(() => {
        if (messageArea.value) {
            messageArea.value.scrollTop = messageArea.value.scrollHeight
        }
    })
}

// 监听消息变化
watch(() => chatStore.messages.length, () => {
    if (currentSessionId.value === chatStore.currentSession?.sessionId) {
        messages.value = [...chatStore.messages]
        scrollToBottom()
    }
})

// 加载会话列表
const loadSessionList = async () => {
    try {
        const staffId = userStore.user?.id
        if (!staffId) return

        const list = await chatApi.getSessionList(staffId)
        chattingList.value = list.filter(s => s.status === 'chatting')
        waitingList.value = list.filter(s => s.status === 'waiting')
        closedList.value = list.filter(s => s.status === 'closed')
    } catch (error) {
        console.error('加载会话列表失败', error)
    }
}

// 加载快捷回复
const loadQuickReplies = async () => {
    try {
        quickReplies.value = await chatApi.getQuickReplies()
    } catch (error) {
        console.error('加载快捷回复失败', error)
    }
}

// 选择客户
const selectCustomer = async (item) => {
    currentSessionId.value = item.sessionId
    currentSession.value = item

    // 不需要断开/重连 WebSocket，只清除本地消息
    chatStore.messages = []

    const staffId = userStore.user?.id
    const staffName = userStore.user?.username

    // 加载历史消息
    try {
        const history = await chatApi.getMessages(item.sessionId)
        const mappedMessages = history.map(msg => ({
            id: msg.id,
            senderType: msg.senderType,
            senderId: msg.senderId,
            senderName: msg.senderName,
            content: msg.content,
            time: msg.createTime,
            isRead: msg.isRead === 1
        }))
        chatStore.messages = mappedMessages
        messages.value = [...mappedMessages]

        // 标记已读
        if (staffId) {
            await chatApi.markAsRead(item.sessionId, staffId)
        }
    }
    catch (error) {
        console.error('加载消息失败', error)
    }

    // 设置会话信息
    chatStore.setCurrentSession({
        sessionId: item.sessionId,
        userName: item.userName,
        staffName: staffName,
        status: item.status
    }, staffName, 'clerk')

    // 已有 WebSocket 连接（Workbench.vue 中已连接），设置回调
    chatStore.setOnMessage(() => {
        messages.value = [...chatStore.messages]
        scrollToBottom()
    })
    
    // 处理新客户接入通知
    chatStore.setOnAllocated((data) => {
        ElMessage.info(data.content || '新客户接入')
        loadSessionList()
    })
    
    // 处理系统消息
    chatStore.setOnSystem((data) => {
        messages.value = [...chatStore.messages]
        scrollToBottom()
    })

    scrollToBottom()
}

// 接受等待中的客户
const acceptCustomer = async (item) => {
    await selectCustomer(item)
    ElMessage.success('已接入客户会话')
}

// 发送消息
const sendMessage = () => {
    if (!inputMessage.value.trim()) return

    const success = chatStore.sendMessage(inputMessage.value)
    if (success) {
        inputMessage.value = ''
        messages.value = [...chatStore.messages]
        scrollToBottom()
    } else {
        ElMessage.error('发送失败')
    }
}

// 使用快捷回复
const useQuickReply = (content) => {
    inputMessage.value = content
}

// 结束会话
const closeSession = async () => {
    if (!currentSessionId.value) return

    try {
        await ElMessageBox.confirm('确定要结束当前会话吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const staffId = userStore.user?.id
        if (staffId) {
            await chatApi.endSession(currentSessionId.value, staffId)
        }

        chatStore.endSession()
        currentSession.value = null
        currentSessionId.value = null
        messages.value = []

        // 刷新列表
        loadSessionList()
        ElMessage.success('会话已结束')
    } catch {
        // 取消
    }
}

// 获取头像文字
const getAvatarText = (name) => {
    if (!name) return '?'
    return name.charAt(0).toUpperCase()
}

// 获取消息样式
const getMessageClass = (msg) => {
    if (msg.senderType === 'system') return 'system-message'
    if (msg.senderType === 'clerk') return 'my-message'
    return 'other-message'
}

// 获取状态类型
const getStatusType = (status) => {
    const types = { waiting: 'warning', chatting: 'success', closed: 'info' }
    return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
    const texts = { waiting: '等待中', chatting: '进行中', closed: '已结束' }
    return texts[status] || status
}

// 格式化时间
const formatTime = (time) => {
    if (!time) return ''
    const date = new Date(time)
    const now = new Date()
    const diff = now - date

    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
    if (diff < 86400000) return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 定时刷新
let refreshTimer = null
let messageTimer = null

// 刷新当前会话消息
const refreshMessages = async () => {
    const sessionId = currentSessionId.value
    if (!sessionId) return

    try {
        const msgList = await chatApi.getMessages(sessionId)
        const newMessages = msgList.map(msg => ({
            id: msg.id,
            senderType: msg.senderType,
            senderId: msg.senderId,
            senderName: msg.senderName,
            content: msg.content,
            time: msg.createTime,
            isRead: msg.isRead === 1
        }))

        // 只在消息数量变化时更新
        if (newMessages.length !== messages.value.length) {
            messages.value = newMessages
            scrollToBottom()
        }
    }
    catch (error) {
        console.error('刷新消息失败', error)
    }
}

// 启动定时刷新
const startRefreshTimers = () => {
    stopRefreshTimers()

    // 每3秒刷新会话列表
    refreshTimer = setInterval(() => {
        loadSessionList()
    }, 3000)

    // 每5秒刷新当前消息
    messageTimer = setInterval(() => {
        refreshMessages()
    }, 5000)
}

// 停止定时刷新
const stopRefreshTimers = () => {
    if (refreshTimer) {
        clearInterval(refreshTimer)
        refreshTimer = null
    }
    if (messageTimer) {
        clearInterval(messageTimer)
        messageTimer = null
    }
}

onMounted(() => {
    loadSessionList()
    loadQuickReplies()
    startRefreshTimers()
    
    // 设置新客户接入通知回调（连接已由 Workbench 全局管理）
    onAllocatedCallback = (data) => {
        ElMessage.info('新客户接入: ' + (data.userName || data.content))
        loadSessionList()
    }
    chatStore.setOnAllocated(onAllocatedCallback)
    
    // 处理系统消息
    onSystemCallback = (data) => {
        loadSessionList()
    }
    chatStore.setOnSystem(onSystemCallback)
})

onUnmounted(() => {
    stopRefreshTimers()
    // 清除回调
    if (onAllocatedCallback) {
        chatStore.clearOnAllocated(onAllocatedCallback)
    }
    if (onSystemCallback) {
        chatStore.clearOnSystem(onSystemCallback)
    }
    // 不在这里断开WebSocket连接，保持全局连接
})
</script>

<style scoped>
.chat-workbench {
    height: calc(100vh - 140px);
    min-height: 500px;
    background: #f5f7fa;
    padding: 20px;
}

.workbench-container {
    display: flex;
    height: 100%;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    overflow: hidden;
}

/* 客户列表 */
.customer-list {
    width: 320px;
    border-right: 1px solid #eee;
    display: flex;
    flex-direction: column;
    background: #fff;
}

.list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #eee;
}

.list-header h3 {
    margin: 0;
    font-size: 16px;
}

.unread-text {
    font-size: 12px;
    color: #909399;
}

.status-stats {
    display: flex;
    padding: 12px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #eee;
}

.stat-item {
    flex: 1;
    text-align: center;
}

.stat-num {
    display: block;
    font-size: 20px;
    font-weight: bold;
}

.stat-label {
    font-size: 12px;
    color: #909399;
}

.stat-item.waiting .stat-num {
    color: #e6a23c;
}

.stat-item.chatting .stat-num {
    color: #67c23a;
}

.stat-item.closed .stat-num {
    color: #909399;
}

.customer-tabs {
    flex: 1;
    overflow: hidden;
}

.customer-tabs :deep(.el-tabs__content) {
    height: calc(100% - 40px);
    overflow-y: auto;
    padding: 8px;
}

.empty-list {
    text-align: center;
    padding: 40px 20px;
    color: #909399;
    font-size: 14px;
}

.customer-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
    margin-bottom: 8px;
    position: relative;
}

.customer-item:hover {
    background: #f5f7fa;
}

.customer-item.active {
    background: #ecf5ff;
}

.customer-item.unread {
    background: #fef0f0;
}

.customer-item.unread.active {
    background: #ecf5ff;
}

.item-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: bold;
    margin-right: 12px;
    flex-shrink: 0;
}

.item-info {
    flex: 1;
    min-width: 0;
}

.item-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;
}

.customer-name {
    font-weight: 500;
    color: #333;
}

.item-time {
    font-size: 11px;
    color: #c0c4cc;
}

.item-preview {
    font-size: 12px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.unread-badge {
    position: absolute;
    right: 12px;
    top: 50%;
    transform: translateY(-50%);
    background: #f56c6c;
    color: white;
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 10px;
    min-width: 18px;
    text-align: center;
}

.waiting-item {
    background: #fef9f3;
}

.waiting-item:hover {
    background: #fdf6ec;
}

/* 聊天窗口 */
.chat-window {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.window-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid #eee;
    background: white;
}

.header-info {
    display: flex;
    align-items: center;
    gap: 12px;
}

.header-info .customer-name {
    font-size: 18px;
}

.window-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background: #f5f7fa;
}

.no-selection {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
}

.empty-messages {
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
}

.message-body {
    line-height: 1.6;
}

.message-body.system {
    font-style: italic;
}

/* 快捷回复 */
.quick-replies {
    padding: 12px 20px;
    background: white;
    border-top: 1px solid #eee;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
}

.quick-label {
    font-size: 12px;
    color: #909399;
    flex-shrink: 0;
}

.quick-item {
    cursor: pointer;
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 输入区域 */
.window-input {
    padding: 16px 20px;
    background: white;
    border-top: 1px solid #eee;
}

.window-input .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
}

.hint {
    font-size: 12px;
    color: #909399;
}
</style>
