<template>
  <div class="personal">
    <div class="container">
      <h1 class="page-title">个人中心</h1>
      
      <el-card class="user-info-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="header-icon">
              <i class="el-icon-user"></i>
            </div>
            <h2>个人信息</h2>
          </div>
        </template>
        <div class="user-avatar-large">
          <div class="avatar-circle">
            <span>{{ userInitial }}</span>
          </div>
        </div>
        <el-descriptions :column="1" border class="user-descriptions">
          <el-descriptions-item label="用户名">{{ userStore.user.username }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ userStore.user.realName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ userStore.user.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userStore.user.email }}</el-descriptions-item>
        </el-descriptions>
        <div class="card-footer">
          <el-button type="primary" @click="openEditDialog">
            <i class="el-icon-edit"></i>
            <span>编辑个人信息</span>
          </el-button>
          <el-button type="primary" @click="openPasswordDialog">
            <i class="el-icon-key"></i>
            <span>修改密码</span>
          </el-button>
          <el-button type="primary" class="logout-button" @click="logout">
            <i class="el-icon-switch-button"></i>
            <span>退出登录</span>
          </el-button>
        </div>
      </el-card>
    </div>
    
    <!-- 编辑个人信息对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑个人信息"
      width="400px"
    >
      <el-form
        :model="editForm"
        :rules="editRules"
        ref="editFormRef"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="editForm.username"
            placeholder="请输入用户名"
            disabled
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input
            v-model="editForm.realName"
            placeholder="请输入姓名"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="editForm.phone"
            placeholder="请输入手机号"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="editForm.email"
            placeholder="请输入邮箱"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateUserInfo">确认修改</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="400px"
    >
      <el-form
        :model="passwordForm"
        :rules="passwordRules"
        ref="passwordFormRef"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updatePassword">确认修改</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { computed, ref, reactive } from 'vue'
import { useUserStore } from '../store/user'
import { userApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 计算用户头像的首字母
const userInitial = computed(() => {
  if (userStore.user && userStore.user.realName) {
    return userStore.user.realName.charAt(0).toUpperCase()
  }
  return 'U'
})

// 编辑对话框状态
const editDialogVisible = ref(false)
const editFormRef = ref(null)

// 编辑表单数据
const editForm = reactive({
  username: '',
  realName: '',
  phone: '',
  email: ''
})

// 编辑表单验证规则
const editRules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 密码对话框状态
const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)

// 密码表单数据
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码表单验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 打开编辑对话框
const openEditDialog = () => {
  const user = userStore.user
  
  // 先重置表单，再设置数据
  if (editFormRef.value) {
    editFormRef.value.resetFields()
  }
  
  // 设置表单数据
  editForm.username = user.username || ''
  editForm.realName = user.realName || ''
  editForm.phone = user.phone || ''
  editForm.email = user.email || ''
  
  editDialogVisible.value = true
}

// 更新个人信息
const updateUserInfo = async () => {
  if (!editFormRef.value) return
  
  try {
    await editFormRef.value.validate()
    
    const userId = userStore.user.id
    const updateData = {
      id: userId,
      realName: editForm.realName,
      phone: editForm.phone,
      email: editForm.email
    }
    
    await userApi.updateProfile(updateData)
    
    // 更新本地存储的用户信息
    userStore.user.realName = editForm.realName
    userStore.user.phone = editForm.phone
    userStore.user.email = editForm.email
    
    ElMessage.success('个人信息修改成功')
    editDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.response?.data || '修改个人信息失败')
  }
}

// 打开密码对话框
const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  if (passwordFormRef.value) {
    passwordFormRef.value.resetFields()
  }
  passwordDialogVisible.value = true
}

// 修改密码
const updatePassword = async () => {
  if (!passwordFormRef.value) return
  
  try {
    await passwordFormRef.value.validate()
    
    const userId = userStore.user.id
    await userApi.changePassword(userId, passwordForm.oldPassword, passwordForm.newPassword)
    
    ElMessage.success('密码修改成功')
    passwordDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.response?.data || '修改密码失败')
  }
}

const logout = async () => {
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
</script>

<style scoped>
.personal {
  padding: 5%;
  background-color: #f8f9fa;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.personal::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 30%, rgba(102, 126, 234, 0.08) 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(118, 75, 162, 0.08) 0%, transparent 40%);
  z-index: 0;
  animation: gradientShift 15s ease-in-out infinite;
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

.container {
  flex: 1;
  position: relative;
  z-index: 1;
  padding: 0 20px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.page-title {
  text-align: center;
  margin-bottom: 5%;
  color: #333;
  font-size: 2rem;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  animation: fadeIn 1s ease-out;
}

.page-title::after {
  content: '';
  display: block;
  width: 100px;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  margin: 20px auto 0;
  border-radius: 2px;
  animation: expand 1.5s ease-out 0.5s both;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

@keyframes expand {
  from {
    width: 0;
  }
  to {
    width: 100px;
  }
}

.user-info-card {
  margin-bottom: 5%;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeIn 1s ease-out;
  background: white;
  border: 1px solid rgba(102, 126, 234, 0.1);
  flex: 0 1 auto;
  max-width: 100%;
}

.user-info-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 5%;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.2rem;
  transition: all 0.3s ease;
}

.header-icon:hover {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.card-header h2 {
  margin: 0;
  color: #333;
  font-size: 1.3rem;
  font-weight: 600;
  transition: color 0.3s ease;
}

.user-avatar-large {
  display: flex;
  justify-content: center;
  margin: 5% 0;
  animation: fadeIn 1s ease-out 0.3s both;
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
  position: relative;
  overflow: hidden;
}

.avatar-circle::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transform: rotate(45deg);
  transition: left 0.6s ease;
  left: -200%;
}

.avatar-circle:hover {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.4);
}

.avatar-circle:hover::before {
  left: 200%;
}

.user-descriptions {
  margin: 0 5% 5%;
  animation: fadeIn 1s ease-out 0.6s both;
}

.user-descriptions .el-descriptions__label {
  font-weight: 600;
  color: #333;
  font-size: 1rem;
  background-color: #f8f9fa;
  border-right: 1px solid rgba(102, 126, 234, 0.1);
}

.user-descriptions .el-descriptions__content {
  color: #666;
  font-size: 1rem;
  background-color: white;
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
}

.user-descriptions .el-descriptions__row:last-child .el-descriptions__content {
  border-bottom: none;
}

.card-footer {
  margin: 0 5% 5%;
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  animation: fadeIn 1s ease-out 0.9s both;
}

.card-footer .el-button {
  padding: 10px 30px;
  border-radius: 30px;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-footer .el-button:first-child {
  background: linear-gradient(135deg, #4CAF50, #45a049);
  border: none;
}

.card-footer .el-button:first-child:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(76, 175, 80, 0.4);
  background: linear-gradient(135deg, #43a047, #388e3c);
}

.logout-button {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}

.logout-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #5a67d8, #6b46c1);
}

.logout-button i {
  transition: transform 0.3s ease;
}

.logout-button:hover i {
  transform: rotate(15deg) scale(1.1);
}

.not-logged-in {
  text-align: center;
  padding: 100px 0;
  background-color: #f9f9f9;
  border-radius: 16px;
  margin-top: 40px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  animation: fadeIn 1s ease-out;
}

.not-logged-in h2 {
  margin-bottom: 20px;
  color: #666;
  font-size: 1.5rem;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 1.8rem;
  }
  
  .card-header {
    padding: 5%;
  }
  
  .user-avatar-large {
    margin: 5% 0;
  }
  
  .avatar-circle {
    width: 100px;
    height: 100px;
    font-size: 2.5rem;
  }
  
  .user-descriptions {
    margin: 0 5% 5%;
  }
  
  .card-footer {
    margin: 0 5% 5%;
  }
}

@media (max-width: 480px) {
  .personal {
    padding: 5%;
  }
  
  .user-info-card {
    padding: 3%;
  }
  
  .card-header h2 {
    font-size: 1.1rem;
  }
  
  .avatar-circle {
    width: 80px;
    height: 80px;
    font-size: 2rem;
  }
  
  .user-descriptions .el-descriptions__label,
  .user-descriptions .el-descriptions__content {
    font-size: 0.95rem;
  }
  
  .logout-button {
    padding: 8px 25px;
    font-size: 0.95rem;
  }
}
</style>