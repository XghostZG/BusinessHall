<template>
  <div class="notification-center">
    <div class="notification-header">
      <h3>通知中心</h3>
      <el-button 
        type="text" 
        size="small" 
        @click="markAllAsRead"
        :disabled="!unreadCount"
      >
        全部标为已读
      </el-button>
    </div>
    
    <div class="notification-tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="全部" name="all">
          <div v-if="loading" class="loading-state">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
          <div v-else-if="notifications.length === 0" class="empty-state">
            <el-empty description="暂无通知" />
          </div>
          <div v-else class="notification-list">
            <div 
              v-for="notification in notifications" 
              :key="notification.id"
              :class="['notification-item', { 'unread': !notification.read }]"
            >
              <div class="notification-content">
                <div class="notification-title">
                  <span :class="['notification-badge', notification.type]"></span>
                  {{ notification.title }}
                </div>
                <div class="notification-text">{{ notification.content }}</div>
                <div class="notification-meta">
                  <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
                  <div class="notification-actions">
                    <el-button 
                      v-if="!notification.read"
                      type="text" 
                      size="small" 
                      @click="markAsRead(notification.id)"
                    >
                      标为已读
                    </el-button>
                    <el-button 
                      type="text" 
                      size="small" 
                      @click="deleteNotification(notification.id)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="未读" name="unread">
          <div v-if="unreadNotifications.length === 0" class="empty-state">
            <el-empty description="暂无未读通知" />
          </div>
          <div v-else class="notification-list">
            <div 
              v-for="notification in unreadNotifications" 
              :key="notification.id"
              class="notification-item unread"
            >
              <div class="notification-content">
                <div class="notification-title">
                  <span :class="['notification-badge', notification.type]"></span>
                  {{ notification.title }}
                </div>
                <div class="notification-text">{{ notification.content }}</div>
                <div class="notification-meta">
                  <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
                  <div class="notification-actions">
                    <el-button 
                      type="text" 
                      size="small" 
                      @click="markAsRead(notification.id)"
                    >
                      标为已读
                    </el-button>
                    <el-button 
                      type="text" 
                      size="small" 
                      @click="deleteNotification(notification.id)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="已读" name="read">
          <div v-if="readNotifications.length === 0" class="empty-state">
            <el-empty description="暂无已读通知" />
          </div>
          <div v-else class="notification-list">
            <div 
              v-for="notification in readNotifications" 
              :key="notification.id"
              class="notification-item"
            >
              <div class="notification-content">
                <div class="notification-title">
                  <span :class="['notification-badge', notification.type]"></span>
                  {{ notification.title }}
                </div>
                <div class="notification-text">{{ notification.content }}</div>
                <div class="notification-meta">
                  <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
                  <div class="notification-actions">
                    <el-button 
                      type="text" 
                      size="small" 
                      @click="deleteNotification(notification.id)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useNotificationStore } from '../store/notification'
import { Loading } from '@element-plus/icons-vue'

const notificationStore = useNotificationStore()
const activeTab = ref('all')

const notifications = computed(() => notificationStore.notifications)
const unreadNotifications = computed(() => notificationStore.unreadNotifications)
const readNotifications = computed(() => notificationStore.readNotifications)
const unreadCount = computed(() => notificationStore.unreadCount)
const loading = computed(() => notificationStore.loading)

onMounted(() => {
  notificationStore.fetchNotifications()
})

const markAsRead = (notificationId) => {
  notificationStore.markAsRead(notificationId)
}

const markAllAsRead = () => {
  notificationStore.markAllAsRead()
}

const deleteNotification = (notificationId) => {
  notificationStore.deleteNotification(notificationId)
}

const formatTime = (time) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.notification-center {
  width: 100%;
  max-height: 450px;
  overflow: hidden;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.notification-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.notification-tabs {
  max-height: 340px;
  overflow-y: auto;
}

.notification-list {
  padding: 8px 0;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  cursor: pointer;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #f0f9eb;
}

.notification-content {
  width: 100%;
}

.notification-title {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.notification-badge {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

.notification-badge.system {
  background-color: #409eff;
}

.notification-badge.business {
  background-color: #67c23a;
}

.notification-badge.personal {
  background-color: #e6a23c;
}

.notification-text {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  line-height: 1.4;
}

.notification-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.notification-time {
  flex: 1;
}

.notification-actions {
  display: flex;
  gap: 8px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #999;
}

.loading-state .el-icon {
  margin-right: 8px;
}

.empty-state {
  padding: 40px 0;
}

/* 自定义滚动条 */
.notification-tabs::-webkit-scrollbar {
  width: 4px;
}

.notification-tabs::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.notification-tabs::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.notification-tabs::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>