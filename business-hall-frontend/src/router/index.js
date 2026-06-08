import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/workbench',
    name: 'Workbench',
    component: () => import('../views/Workbench.vue'),
    redirect: '/workbench/appointment',
    children: [
      {
        path: 'appointment',
        name: 'ClerkAppointment',
        component: () => import('../views/worker/AppointmentManage.vue')
      },
      {
        path: 'chat',
        name: 'ClerkChat',
        component: () => import('../views/worker/ChatWorkbench.vue')
      },
      {
        path: 'attendance',
        name: 'ClerkAttendance',
        component: () => import('../views/worker/EmployeeAttendance.vue')
      }
    ]
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    redirect: '/admin/user',
    children: [
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('../views/admin/UserManage.vue')
      },
      {
        path: 'time-slot',
        name: 'TimeSlotManage',
        component: () => import('../views/admin/TimeSlotManage.vue')
      },
      {
        path: 'time-slot-template',
        name: 'TimeSlotTemplateManage',
        component: () => import('../views/admin/TimeSlotTemplateManage.vue')
      },
      {
        path: 'appointment',
        name: 'AppointmentManage',
        component: () => import('../views/admin/AppointmentManage.vue')
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('../views/admin/Statistics.vue')
      },
      {
        path: 'resource',
        name: 'ResourceManage',
        component: () => import('../views/admin/ResourceManage.vue')
      },
      {
        path: 'approval',
        name: 'ApprovalCenter',
        component: () => import('../views/admin/ApprovalCenter.vue')
      },
      {
        path: 'attendance',
        name: 'AttendanceManage',
        component: () => import('../views/admin/AttendanceManage.vue')
      }
    ]
  },
  {
    path: '/',
    component: () => import('../components/Layout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('../views/Home.vue')
      },
      {
        path: 'appointment',
        name: 'Appointment',
        component: () => import('../views/Appointment.vue')
      },
      {
        path: 'personal',
        name: 'Personal',
        component: () => import('../views/Personal.vue')
      },
      {
        path: 'my-appointments',
        name: 'MyAppointments',
        component: () => import('../views/MyAppointments.vue')
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue')
      },
      {
        path: 'complaint',
        name: 'Complaint',
        component: () => import('../views/Complaint.vue')
      },
      {
        path: 'my-complaints',
        name: 'MyComplaints',
        component: () => import('../views/MyComplaints.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  // 初始化用户信息
  userStore.initUser()
  
  // 从 localStorage 直接获取用户信息作为备用检查
  const savedUser = localStorage.getItem('user') || sessionStorage.getItem('user')
  let userRole = userStore.userRole
  let isLoggedIn = userStore.isLoggedIn
  
  // 如果 store 中没有 user，但 localStorage 中有，尝试解析
  if (!userStore.user && savedUser) {
    try {
      const parsedUser = JSON.parse(savedUser)
      userRole = parsedUser.role || ''
      isLoggedIn = true
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }
  
  console.log('路由守卫 - 用户角色:', userRole, '已登录:', isLoggedIn, '目标路径:', to.path)
  
  // 检查是否已登录
  if (isLoggedIn) {
    const role = userRole?.toLowerCase() || ''
    
    // 已登录，根据角色跳转到相应页面
    if (to.path === '/' || to.path === '/home') {
      if (role === 'admin') {
        console.log('管理员跳转至 /admin/user')
        next('/admin/user')
        return
      } else if (role === 'clerk') {
        console.log('营业员跳转至 /workbench')
        next('/workbench')
        return
      } else if (role === 'user') {
        console.log('普通用户继续导航')
      } else {
        console.log('其他角色:', userRole, '，继续导航')
      }
    }
    
    // 权限控制：检查用户角色是否有权限访问当前路由
    if (to.path.startsWith('/admin') && role !== 'admin') {
      console.log('无管理权限，重定向到 /home')
      next('/home')
      return
    }
    if (to.path.startsWith('/workbench') && role !== 'clerk' && role !== 'admin') {
      console.log('无营业员权限，重定向到 /home')
      next('/home')
      return
    }
  } else {
    // 未登录，检查是否需要登录
    const requireAuth = ['/personal', '/my-appointments', '/appointment', '/workbench', '/admin']
    if (requireAuth.some(path => to.path.startsWith(path))) {
      console.log('需要登录，重定向到 /login')
      next('/login')
      return
    }
  }
  // 继续导航
  next()
})

export default router