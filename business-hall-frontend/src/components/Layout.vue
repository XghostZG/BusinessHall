<template>
  <div class="layout">
    <!-- 顶部导航栏 -->
    <div class="navbar">
      <div class="container">
        <div class="navbar-content">
          <div class="navbar-logo" @click="toHome">
            <h2>营业厅业务预约系统</h2>
          </div>
          <el-menu :default-active="activeMenu" mode="horizontal" class="navbar-menu">
            <el-menu-item index="/home" @click="toHome">
              <template #default>
                <span class="menu-text">首页</span>
              </template>
            </el-menu-item>
            <el-menu-item index="/appointment" @click="toAppointment">
              <template #default>
                <span class="menu-text">预约</span>
              </template>
            </el-menu-item>
            <el-menu-item index="/my-appointments" @click="toMyAppointments">
              <template #default>
                <span class="menu-text">我的预约</span>
              </template>
            </el-menu-item>
            <el-menu-item index="/chat" @click="toChat">
              <template #default>
                <span class="menu-text">在线客服</span>
              </template>
            </el-menu-item>
            <el-menu-item index="/complaint" @click="toComplaint">
              <template #default>
                <span class="menu-text">投诉</span>
              </template>
            </el-menu-item>
          </el-menu>
          <div class="navbar-right">
            
            <el-dropdown @command="handleDropdownCommand" trigger="click">
              <div class="user-profile">
                <div class="user-avatar">{{ userAvatar }}</div>
                <span class="user-name">{{ userName }}</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu class="user-dropdown-menu">
                  <el-dropdown-item command="personal" class="dropdown-item">
                    <el-icon><User /></el-icon>
                    <span>个人中心</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="my-complaints" class="dropdown-item">
                    <el-icon><Warning /></el-icon>
                    <span>我的投诉</span>
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isLoggedIn" command="logout" class="dropdown-item">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 中间内容区域 -->
    <div class="content">
      <router-view />
    </div>
    
    <!-- 底部区域 -->
    <div class="footer">
      <div class="container">
        <div class="footer-content">
          <div class="footer-info">
            <h3>营业厅业务预约系统</h3>
            <p>提供便捷的业务预约服务，减少排队等待时间</p>
          </div>
          <div class="footer-links">
            <h4>快速链接</h4>
              <ul>
              <li><a href="/home">首页</a></li>
              <li><a href="/appointment">立即预约</a></li>
              <li><a href="/my-appointments">我的预约</a></li>
              <li><a href="/my-complaints">我的投诉</a></li>
              <li><a href="/chat">在线客服</a></li>
              <li><a href="/complaint">投诉</a></li>
              <li><a href="/personal">个人中心</a></li>
            </ul>
          </div>
          <div class="footer-contact">
            <h4>联系我们</h4>
            <p>电话：1234567890</p>
            <p>邮箱：service@example.com</p>
            <p>地址：北京市朝阳区XX大厦</p>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; 2026 营业厅业务预约系统. 保留所有权利.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { useNotificationStore } from '../store/notification'
import { User, ArrowDown, SwitchButton, Bell } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import NotificationCenter from './NotificationCenter.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

// 初始化用户状态
userStore.initUser()

// 通知相关
const showNotificationCenter = ref(false)

// 初始化通知
onMounted(() => {
  if (userStore.isLoggedIn) {
    notificationStore.fetchUnreadCount()
  }
})

const toggleNotificationCenter = () => {
  showNotificationCenter.value = !showNotificationCenter.value
  if (showNotificationCenter.value) {
    notificationStore.fetchNotifications()
  }
}

// 计算当前激活的菜单
const activeMenu = computed(() => {
  return route.path
})

// 用户信息
const userAvatar = ref('👤')
const userName = ref('个人中心')

const toHome = () => {
  router.push('/home')
}

const toAppointment = () => {
  router.push('/appointment')
}

const toMyAppointments = () => {
  router.push('/my-appointments')
}

const toChat = () => {
  router.push('/chat')
}

const toComplaint = () => {
  router.push('/complaint')
}

const toMyComplaints = () => {
  router.push('/my-complaints')
}

const toMyConsultations = () => {
  router.push('/my-consultations')
}

const toPersonal = () => {
  router.push('/personal')
}

// 处理下拉菜单命令
const handleDropdownCommand = async (command) => {
  if (command === 'personal') {
    if (userStore.isLoggedIn) {
      toPersonal()
    } else {
      router.push('/login')
    }
  } else if (command === 'my-consultations') {
    toMyConsultations()
  } else if (command === 'my-complaints') {
    toMyComplaints()
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      await userStore.logout()
      router.push('/login')
      ElMessage.success('退出登录成功')
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('退出登录失败')
      }
    }
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  scroll-behavior: smooth;
  width: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
}

/* 顶部导航栏 */
.navbar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
  padding: 0;
  width: 100%;
  height: 10%;
  min-height: 70px;
  display: flex;
  align-items: center;
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  position: relative;
  z-index: 100;
  overflow: visible;
}

.navbar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 10% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 20%),
    radial-gradient(circle at 80% 80%, rgba(255, 255, 255, 0.08) 0%, transparent 25%);
  z-index: 0;
  animation: gradientShift 8s ease-in-out infinite;
}

@keyframes gradientShift {
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

.navbar:hover {
  box-shadow: 0 8px 28px rgba(102, 126, 234, 0.5);
  /* 移除 translateY 避免与 content 区域产生间隙 */
}

.navbar-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
  padding: 0 30px;
}

.navbar-logo {
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  padding: 15px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.navbar-logo::before {
  content: '🏢';
  font-size: 1.8rem;
  transition: transform 0.4s ease;
}

.navbar-logo:hover::before {
  transform: scale(1.2) rotate(10deg);
}

.navbar-logo h2 {
  font-size: 1.4rem;
  font-weight: 700;
  color: white;
  margin: 0;
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.navbar-logo:hover h2 {
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  transform: scale(1.05);
}

.navbar-logo::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.6s ease;
}

.navbar-logo:hover::after {
  left: 100%;
}

.navbar-menu {
  flex: 1;
  justify-content: center;
  gap: 10px;
  border-bottom: none !important;
  margin: 0 50px;
  background: transparent !important;
}

.navbar-menu .el-menu {
  background: transparent !important;
  border-bottom: none !important;
}

/* 确保所有Element UI菜单相关元素都是透明背景 */
.el-menu {
  background: transparent !important;
  border: none !important;
}

.el-menu--horizontal {
  border: none !important;
  background: transparent !important;
}

.el-menu--horizontal > .el-menu-item {
  background: transparent !important;
}

.el-menu--horizontal > .el-menu-item:hover {
  background: transparent !important;
}

.el-menu--horizontal > .el-menu-item.is-active {
  background: transparent !important;
}

.navbar-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.9);
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.3s ease;
  position: relative;
  padding: 0 30px;
  height: 70px;
  line-height: 70px;
  overflow: hidden;
  margin: 0 5px;
}

.navbar-menu .el-menu-item:hover {
  color: white;
  background-color: transparent !important;
  box-shadow: none !important;
}

.navbar-menu .el-menu-item.is-active {
  color: white !important;
  background-color: transparent !important;
  box-shadow: none !important;
  border-bottom: none !important;
}

/* 确保Element UI默认的蓝色下划线不显示 */
.el-menu--horizontal .el-menu-item.is-active {
  border-bottom: none !important;
}

.el-menu-item.is-active::before {
  display: none !important;
}

.navbar-menu .el-menu-item::after {
  content: '';
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 3px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.8), white);
  transition: width 0.4s ease;
  border-radius: 2px;
}

.navbar-menu .el-menu-item:hover::after,
.navbar-menu .el-menu-item.is-active::after {
  width: 80%;
}

.menu-text {
  position: relative;
  z-index: 1;
  display: inline-block;
  transition: all 0.3s ease;
}

.navbar-menu .el-menu-item:hover .menu-text {
  transform: scale(1.08);
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-icon {
  position: relative;
  cursor: pointer;
  padding: 12px;
  border-radius: 50%;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.notification-icon:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.1);
}

.bell-icon {
  font-size: 1.2rem;
  color: white;
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

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  border-radius: 50px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.user-profile:hover {
  background: rgba(255, 255, 255, 0.25);
  /* 移除 translateY 避免导航栏高度变化 */
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.25);
  border-color: rgba(255, 255, 255, 0.3);
}

.user-profile::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.6s ease;
}

.user-profile:hover::before {
  left: 100%;
}

.user-avatar {
  font-size: 1.8rem;
  transition: transform 0.4s ease, color 0.3s ease;
  position: relative;
  z-index: 1;
  background: white;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.user-profile:hover .user-avatar {
  transform: scale(1.2) rotate(15deg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.user-name {
  font-size: 1rem;
  font-weight: 500;
  color: white;
  transition: color 0.3s ease, transform 0.3s ease;
  position: relative;
  z-index: 1;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.user-profile:hover .user-name {
  transform: scale(1.05);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* 下拉菜单样式 */
.user-dropdown-menu {
  border-radius: 16px;
  overflow: hidden;
  border: none;
  animation: dropdownFadeIn 0.3s ease-out;
  background: white;
  backdrop-filter: blur(10px);
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

.dropdown-item {
  transition: all 0.3s ease;
  padding: 15px 28px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item:hover {
  background-color: #f8f9fa;
  color: #667eea;
  transform: translateX(8px);
}

.dropdown-item i {
  font-size: 1.2rem;
  transition: transform 0.3s ease;
  width: 20px;
  text-align: center;
}

.dropdown-item:hover i {
  transform: scale(1.2) rotate(10deg);
}

/* 中间内容区域 */
.content {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  overflow-y: auto;
  flex: 1;
  background-color: #f8f9fa;
  z-index: 1;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}

.content > * {
  flex: 1;
  min-height: 100%;
}

/* 底部区域 */
.footer {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 100%;
  box-sizing: border-box;
  cursor: pointer;
  height: 20%;
  min-height: 150px;
  transition: all 0.3s ease;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
}

.footer:hover {
  transform: translateY(-2px);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.15);
}

.footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 10% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 20%),
    radial-gradient(circle at 80% 80%, rgba(255, 255, 255, 0.08) 0%, transparent 25%),
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.05) 0%, transparent 30%);
  z-index: 0;
  animation: gradientShift 10s ease-in-out infinite;
}

@keyframes gradientShift {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

.footer-content {
  display: flex;
  justify-content: space-around;
  align-items: center;
  position: relative;
  z-index: 1;
  width: 100%;
  flex: 1;
  box-sizing: border-box;
  cursor: pointer;
}

.footer-info {
  flex: 1;
  max-width: 30%;
  transition: transform 0.4s ease, box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  cursor: pointer;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.footer-info:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  background: rgba(255, 255, 255, 0.08);
}

.footer-info h3 {
  font-size: 1.1rem;
  margin-bottom: 8px;
  font-weight: 700;
  transition: color 0.3s ease, transform 0.3s ease;
  position: relative;
  z-index: 1;
}

.footer-info:hover h3 {
  color: #f8f9fa;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  transform: scale(1.05);
}

.footer-info p {
  line-height: 1.6;
  opacity: 0.9;
  transition: opacity 0.3s ease, transform 0.3s ease;
  position: relative;
  z-index: 1;
}

.footer-info:hover p {
  opacity: 1;
  transform: translateY(3px);
}

.footer-links {
  flex: 1;
  max-width: 30%;
  transition: transform 0.4s ease, box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  cursor: pointer;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.footer-links:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  background: rgba(255, 255, 255, 0.08);
}

.footer-links h4,
.footer-contact h4 {
  font-size: 1rem;
  font-weight: 600;
  transition: color 0.3s ease, transform 0.3s ease;
  position: relative;
  z-index: 1;
}

.footer-links:hover h4,
.footer-contact:hover h4 {
  color: #f8f9fa;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  transform: scale(1.05);
}

.footer-links ul {
  list-style: none;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px 20px;
  width: 100%;
  max-width: 350px;
  position: relative;
  z-index: 1;
}

.footer-links li {
  margin-bottom: 0;
  transition: transform 0.3s ease;
}

.footer-links li:hover {
  transform: translateX(5px);
}

.footer-links a {
  color: white;
  text-decoration: none;
  opacity: 0.9;
  transition: all 0.3s ease;
  display: block;
  position: relative;
  padding: 6px 0 6px 10px;
  font-size: 0.85rem;
  white-space: nowrap;
  text-align: left;
  cursor: pointer;
  border-radius: 6px;
}

.footer-links a::before {
  content: '→';
  position: absolute;
  left: -15px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.3s ease, transform 0.3s ease;
  font-weight: bold;
}

.footer-links a:hover {
  opacity: 1;
  transform: translateX(8px);
  color: #f8f9fa;
  background: rgba(255, 255, 255, 0.1);
  padding-left: 15px;
}

.footer-links a:hover::before {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
  color: #f8f9fa;
}

.footer-links a::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 12px;
  width: 0;
  height: 2px;
  background-color: white;
  transition: width 0.3s ease;
  border-radius: 1px;
}

.footer-links a:hover::after {
  width: 85%;
}

.footer-contact {
  flex: 1;
  max-width: 30%;
  transition: transform 0.4s ease, box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  cursor: pointer;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.footer-contact:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  background: rgba(255, 255, 255, 0.08);
}

.footer-contact p {
  margin-bottom: 8px;
  opacity: 0.9;
  line-height: 1.4;
  transition: opacity 0.3s ease, transform 0.3s ease;
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 1;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 0.85rem;
}

.footer-contact p::before {
  content: '📞';
  font-size: 1.1rem;
  transition: transform 0.3s ease;
}

.footer-contact p:nth-child(2)::before {
  content: '📧';
}

.footer-contact p:nth-child(3)::before {
  content: '📍';
}

.footer-contact:hover p {
  opacity: 1;
  transform: translateX(8px);
  color: #f8f9fa;
  background: rgba(255, 255, 255, 0.1);
}

.footer-contact:hover p::before {
  transform: scale(1.1) rotate(5deg);
}

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  text-align: center;
  opacity: 0.8;
  font-size: 0.8rem;
  transition: opacity 0.3s ease, transform 0.3s ease;
  position: relative;
  z-index: 1;
  cursor: pointer;
  margin-top: 8px;
}

.footer-bottom:hover {
  opacity: 1;
  color: #f8f9fa;
  transform: translateY(-2px);
}

/* 容器 */
.container {
  width: 100%;
  padding: 0 20px;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
  background: transparent !important;
}

/* 响应式设计 */

@media (max-width: 768px) {
  .navbar {
    min-height: 60px;
  }
  
  .navbar-content {
    padding: 0 20px;
  }
  
  .navbar-logo {
    gap: 8px;
    padding: 10px 0;
  }
  
  .navbar-logo::before {
    font-size: 1.4rem;
  }
  
  .navbar-logo h2 {
    font-size: 1.1rem;
  }
  
  .navbar-menu {
    display: none;
  }
  
  .user-name {
    display: none;
  }
  
  .user-profile {
    padding: 8px 16px;
  }
  
  .user-avatar {
    width: 32px;
    height: 32px;
    font-size: 1.4rem;
  }
  
  .footer-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 20px;
  }
  
  .footer-info,
  .footer-links,
  .footer-contact {
    width: 100%;
    max-width: 400px;
  }
  
  .footer-links a {
    padding-left: 0;
    text-align: center;
  }
  
  .footer-links a::before {
    display: none;
  }
  
  .footer-links a::after {
    left: 50%;
    transform: translateX(-50%);
  }
  
  .footer-links ul {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  
  .footer-contact p {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .navbar-content {
    padding: 0 15px;
  }
  
  .navbar-logo h2 {
    font-size: 1rem;
  }
  
  .navbar-logo::before {
    font-size: 1.2rem;
  }
  
  .user-profile {
    padding: 6px 12px;
  }
  
  .user-avatar {
    width: 28px;
    height: 28px;
    font-size: 1.2rem;
  }
  
  .footer {
    padding: 20px;
  }
  
  .footer-links ul {
    gap: 8px;
  }
}
</style>