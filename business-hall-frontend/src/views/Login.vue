<template>
  <div class="login">
    <div class="login-container">
      <div class="login-header">
        <div class="logo">
          <div class="logo-icon">🏢</div>
          <h1>营业厅业务预约系统</h1>
        </div>
        <h2>用户登录</h2>
        <p class="login-subtitle">请输入您的账号信息进行登录</p>
      </div>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            :class="{ 'input-focus': isUsernameFocus }"
            @focus="isUsernameFocus = true"
            @blur="isUsernameFocus = false"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            :class="{ 'input-focus': isPasswordFocus }"
            @focus="isPasswordFocus = true"
            @blur="isPasswordFocus = false"
          />
        </el-form-item>
        <div class="form-actions">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <!-- <el-button type="text" @click="forgotPassword" class="forgot-password">忘记密码？</el-button> -->
        </div>
        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            @click="login"
            :loading="loading"
            :disabled="loading"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
        <div class="register-link">
          <span>还没有账号？</span>
          <el-button type="text" @click="toRegister" class="register-button">立即注册</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../api'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const form = ref({
  username: '',
  password: ''
})
const formRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)
const isUsernameFocus = ref(false)
const isPasswordFocus = ref(false)

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20之间', trigger: 'blur' }
  ]
}

const login = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const response = await userApi.login(form.value)
    const user = response.user
    userStore.setUser(user)
    userStore.setToken('mock-token')
    
    if (user.role === 'admin') {
      router.push('/admin/user')
    } else if (user.role === 'clerk') {
      router.push('/workbench')
    } else {
      router.push('/home')
    }
  } catch (error) {
    ElMessage.error(error.response?.data || '登录失败')
  } finally {
    loading.value = false
  }
}

const toRegister = () => {
  router.push('/register')
}

const forgotPassword = () => {
  ElMessage.info('忘记密码功能暂未实现')
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-family: 'Arial', sans-serif;
}

.login-container {
  width: 400px;
  padding: 40px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.login-container:hover {
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  transform: translateY(-5px);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 10px;
  animation: pulse 2s infinite;
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

.login-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.login-header h2 {
  font-size: 20px;
  font-weight: 500;
  color: #555;
  margin-bottom: 5px;
}

.login-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.login-form {
  margin-top: 20px;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-input {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.el-input:focus-within {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.input-focus {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;
}

.forgot-password {
  color: #667eea;
  transition: color 0.3s ease;
}

.forgot-password:hover {
  color: #764ba2;
}

.login-button {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.login-button:active {
  transform: translateY(0);
}

.register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.register-button {
  color: #667eea;
  margin-left: 5px;
  transition: color 0.3s ease;
}

.register-button:hover {
  color: #764ba2;
}

@media (max-width: 480px) {
  .login-container {
    width: 90%;
    padding: 25px;
  }
  
  .logo-icon {
    font-size: 36px;
  }
  
  .login-header h1 {
    font-size: 20px;
  }
  
  .login-header h2 {
    font-size: 18px;
  }
  
  .login-button {
    height: 40px;
    font-size: 14px;
  }
}
</style>