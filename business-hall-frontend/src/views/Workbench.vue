<template>
  <div class="workbench">
    <el-container class="workbench-container">
      <el-aside width="240px" class="workbench-sidebar">
        <div class="sidebar-header">
          <h3 class="sidebar-title">营业员工作台</h3>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="workbench-menu"
          router
        >
          <el-menu-item index="/workbench/appointment">
            <el-icon class="menu-icon"><i class="el-icon-s-order"></i></el-icon>
            <span>预约处理</span>
          </el-menu-item>
          <el-menu-item index="/workbench/chat">
            <el-icon class="menu-icon"><i class="el-icon-chat-dot-round"></i></el-icon>
            <span>在线客服</span>
          </el-menu-item>
          <el-menu-item index="/workbench/attendance">
            <el-icon class="menu-icon"><i class="el-icon-calendar"></i></el-icon>
            <span>我的考勤</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container class="workbench-content">
        <el-header class="workbench-header">
          <div class="header-content">
            <div class="title-container">
              <el-icon class="title-icon"><i class="el-icon-s-operation"></i></el-icon>
              <h2 class="header-title">智能营业厅管理系统</h2>
              <span class="title-subtitle">Service Desk</span>
            </div>
            <div class="header-actions">
              <el-dropdown trigger="click" class="user-dropdown">
                <div class="user-info">
                  <el-avatar size="small" :src="userAvatar">{{ userInitial }}</el-avatar>
                  <span class="user-name">{{ userName }}</span>
                  <el-icon class="dropdown-icon"><i class="el-icon-arrow-down"></i></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="showPersonalInfo = true">
                      <el-icon><i class="el-icon-user"></i></el-icon>
                      <span>个人信息</span>
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="logout">
                      <el-icon><i class="el-icon-switch-button"></i></el-icon>
                      <span>退出登录</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-header>
        <el-main class="workbench-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
    
    <!-- 个人信息对话框 -->
    <el-dialog
      v-model="showPersonalInfo"
      title="个人信息"
      width="600px"
      class="personal-dialog"
    >
      <div class="personal-info-content">
        <div class="user-avatar-large">
          <div class="avatar-circle">
            <span>{{ userInitial }}</span>
          </div>
        </div>
        <el-descriptions :column="1" border class="user-descriptions">
          <el-descriptions-item label="用户名">{{ userStore.user?.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ userStore.user?.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ userStore.user?.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userStore.user?.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ userStore.user?.role || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showPersonalInfo = false" class="close-button">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { useNotificationStore } from '../store/notification'
import { useChatStore } from '../store/chat'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import NotificationCenter from '../components/NotificationCenter.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const chatStore = useChatStore()
const showNotificationCenter = ref(false)
const showPersonalInfo = ref(false)

// 计算当前激活的菜单
const activeMenu = computed(() => {
  return route.path
})

// 计算属性：用户信息
const userName = computed(() => {
  return userStore.user?.username || '营业员'
})

const userInitial = computed(() => {
  return userName.value.charAt(0).toUpperCase()
})

const userAvatar = computed(() => {
  return userStore.user?.avatar || ''
})

// 通知相关
const toggleNotificationCenter = () => {
  showNotificationCenter.value = !showNotificationCenter.value
  if (showNotificationCenter.value) {
    notificationStore.fetchNotifications()
  }
}

// 监听路由变化，关闭通知中心
watch(() => route.path, () => {
  showNotificationCenter.value = false
})

// 保存回调引用，用于组件销毁时清除
let onAllocatedCallback = null

// 初始化通知和WebSocket连接
onMounted(() => {
  if (userStore.isLoggedIn) {
    notificationStore.fetchUnreadCount()
    
    // 营业员身份则建立全局WebSocket连接
    if (userStore.user?.role === 'clerk') {
      const userId = userStore.user.id
      const userNameStr = userStore.user.username
      if (userId && userNameStr) {
        chatStore.connect(userId, userNameStr, 'clerk')
        
        // 设置新客户接入通知回调
        onAllocatedCallback = (data) => {
          const customerName = data.userName || data.content
          const message = `有新客户接入: ${customerName}`
          ElMessage({
            message: message,
            type: 'info',
            duration: 5000,
            onClick: () => {
              // 点击消息跳转到客服页面
              if (route.path !== '/workbench/chat') {
                router.push('/workbench/chat')
              }
            }
          })
        }
        chatStore.setOnAllocated(onAllocatedCallback)
      }
    }
  }
})

// 组件销毁时断开WebSocket并清除回调
onUnmounted(() => {
  if (userStore.user?.role === 'clerk') {
    // 清除回调
    if (onAllocatedCallback) {
      chatStore.clearOnAllocated(onAllocatedCallback)
    }
    chatStore.disconnect()
  }
})

// 退出登录
const logout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 断开营业员WebSocket连接
    if (userStore.user?.role === 'clerk') {
      chatStore.disconnect()
    }
    
    await userStore.logout()
    router.push('/login')
    ElMessage.success('退出登录成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退出登录失败')
    }
  }
}
</script>

<style scoped>
.workbench {
  height: 100vh;
  background-color: #f5f7fa;
}

.workbench-container {
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.workbench-sidebar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  text-align: center;
}

.sidebar-title {
  margin: 0;
  color: white;
  font-size: 1.2rem;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  animation: fadeIn 1s ease-out;
}

.workbench-menu {
  flex: 1;
  background: transparent;
  border-right: none;
  margin-top: 20px;
}

.workbench-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.8);
  margin: 0 10px 10px;
  border-radius: 8px;
  transition: all 0.3s ease;
  height: 50px;
  line-height: 50px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.workbench-menu .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
  transform: translateX(5px);
}

.workbench-menu .el-menu-item.is-active {
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.menu-icon {
  font-size: 1.2rem;
  margin-right: 0;
}

/* 内容区域 */
.workbench-content {
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

/* 头部 */
.workbench-header {
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  padding: 0;
  height: 70px;
  z-index: 10;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  height: 100%;
}

.title-container {
  display: flex;
  align-items: center;
  gap: 15px;
  animation: fadeIn 1s ease-out;
}

.title-icon {
  font-size: 1.8rem;
  color: #667eea;
  text-shadow: 0 2px 4px rgba(102, 126, 234, 0.3);
  animation: pulse 2s infinite;
}

.header-title {
  margin: 0;
  color: #333;
  font-size: 1.4rem;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.title-subtitle {
  font-size: 0.9rem;
  color: #667eea;
  font-weight: 600;
  background: linear-gradient(90deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-icon {
  position: relative;
  cursor: pointer;
  padding: 12px;
  border-radius: 50%;
  transition: all 0.3s ease;
  background: rgba(102, 126, 234, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(102, 126, 234, 0.2);
}

.notification-icon:hover {
  background: rgba(102, 126, 234, 0.25);
  transform: scale(1.1);
}

.bell-icon {
  font-size: 1.2rem;
  color: #667eea;
  transition: transform 0.3s ease;
}

.notification-icon:hover .bell-icon {
  transform: rotate(15deg);
}

.notification-badge {
  --el-badge-font-size: 11px;
  --el-badge-size: 18px;
  --el-badge-background-color: #f56c6c;
}

.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 30px;
  margin-top: 8px;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  overflow: hidden;
  animation: dropdownFadeIn 0.3s ease-out;
  width: 350px;
  max-width: calc(100vw - 60px);
}

@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-15px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.user-info:hover {
  background-color: rgba(102, 126, 234, 0.1);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.dropdown-icon {
  font-size: 12px;
  color: #666;
  transition: transform 0.3s ease;
}

.user-dropdown:hover .dropdown-icon {
  transform: rotate(180deg);
}

/* 主内容区 */
.workbench-main {
  flex: 1;
  padding: 0;
  overflow-y: auto;
  background-color: #f5f7fa;
}

/* 个人信息对话框 */
.personal-dialog {
  border-radius: 12px;
  overflow: hidden;
}

.personal-info-content {
  padding: 20px 0;
}

.user-avatar-large {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.avatar-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 3rem;
  font-weight: bold;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  transition: all 0.4s ease;
}

.avatar-circle:hover {
  transform: scale(1.1);
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.4);
}

.user-descriptions {
  margin: 0 20px;
}

.user-descriptions .el-descriptions__label {
  font-weight: 600;
  color: #333;
  font-size: 14px;
  background-color: #f8f9fa;
  border-right: 1px solid rgba(102, 126, 234, 0.1);
}

.user-descriptions .el-descriptions__content {
  color: #666;
  font-size: 14px;
  background-color: white;
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
}

.close-button {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 6px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.close-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

@media (max-width: 768px) {
  .workbench-sidebar {
    width: 200px !important;
  }
  
  .sidebar-title {
    font-size: 1rem;
  }
  
  .workbench-menu .el-menu-item {
    font-size: 0.9rem;
    height: 45px;
    line-height: 45px;
  }
  
  .header-content {
    padding: 0 20px;
  }
  
  .title-container {
    gap: 10px;
  }
  
  .title-icon {
    font-size: 1.5rem;
  }
  
  .header-title {
    font-size: 1.1rem;
  }
  
  .title-subtitle {
    font-size: 0.8rem;
  }
}

@media (max-width: 480px) {
  .workbench-sidebar {
    width: 180px !important;
  }
  
  .workbench-menu .el-menu-item span {
    font-size: 0.85rem;
  }
  
  .menu-icon {
    font-size: 1rem;
  }
}
</style>
