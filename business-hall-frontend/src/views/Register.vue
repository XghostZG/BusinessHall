<template>
  <div class="register">
    <div class="register-container">
      <div class="register-header">
        <div class="logo">
          <div class="logo-icon">🏢</div>
          <h1>营业厅业务预约系统</h1>
        </div>
        <h2>用户注册</h2>
        <p class="register-subtitle">请填写以下信息完成注册</p>
      </div>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0" class="register-form">
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
        <el-form-item prop="realName">
          <el-input
            v-model="form.realName"
            placeholder="请输入姓名"
            prefix-icon="Avatar"
            :class="{ 'input-focus': isRealNameFocus }"
            @focus="isRealNameFocus = true"
            @blur="isRealNameFocus = false"
          />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            prefix-icon="Phone"
            :class="{ 'input-focus': isPhoneFocus }"
            @focus="isPhoneFocus = true"
            @blur="isPhoneFocus = false"
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            prefix-icon="Message"
            :class="{ 'input-focus': isEmailFocus }"
            @focus="isEmailFocus = true"
            @blur="isEmailFocus = false"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="register-button"
            @click="register"
            :loading="loading"
            :disabled="loading"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
        <div class="login-link">
          <span>已有账号？</span>
          <el-button type="text" @click="toLogin" class="login-button">立即登录</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = ref({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: ''
})
const formRef = ref(null)
const loading = ref(false)
const isUsernameFocus = ref(false)
const isPasswordFocus = ref(false)
const isRealNameFocus = ref(false)
const isPhoneFocus = ref(false)
const isEmailFocus = ref(false)

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20之间', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在2-20之间', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const register = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const registerData = { ...form.value }
    delete registerData.role
    await userApi.register(registerData)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.response?.data || '注册失败')
  } finally {
    loading.value = false
  }
}

const toLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-family: 'Arial', sans-serif;
}

.register-container {
  width: 400px;
  padding: 40px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.register-container:hover {
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  transform: translateY(-5px);
}

.register-header {
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

.register-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.register-header h2 {
  font-size: 20px;
  font-weight: 500;
  color: #555;
  margin-bottom: 5px;
}

.register-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.register-form {
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

.register-button {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.register-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.register-button:active {
  transform: translateY(0);
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.login-button {
  color: #667eea;
  margin-left: 5px;
  transition: color 0.3s ease;
}

.login-button:hover {
  color: #764ba2;
}

@media (max-width: 480px) {
  .register-container {
    width: 90%;
    padding: 25px;
  }
  
  .logo-icon {
    font-size: 36px;
  }
  
  .register-header h1 {
    font-size: 20px;
  }
  
  .register-header h2 {
    font-size: 18px;
  }
  
  .register-button {
    height: 40px;
    font-size: 14px;
  }
}
</style>