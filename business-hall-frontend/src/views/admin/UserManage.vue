<template>
  <div class="user-manage">
    <h2 class="page-title">用户管理</h2>
    
    <div class="page-header">
      <el-button type="primary" @click="openAddClerkDialog" class="add-btn">新增营业员</el-button>
    </div>
    
    <el-card class="search-card" shadow="hover">
      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="用户名">
            <el-input v-model="searchForm.username" placeholder="请输入用户名" style="width: 200px" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="searchForm.role" placeholder="选择角色" style="width: 120px">
              <el-option label="客户" value="user" />
              <el-option label="营业员" value="clerk" />
              <el-option label="管理员" value="admin" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="选择状态" style="width: 100px">
              <el-option label="启用" value="1" />
              <el-option label="禁用" value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchUsers" class="search-btn">查询</el-button>
            <el-button @click="resetSearch" class="reset-btn">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    
    <el-card class="table-card" shadow="hover">
      <el-table :data="users" style="width: 100%" class="user-table">
        <el-table-column prop="id" label="用户ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" align="center" />
        <el-table-column prop="realName" label="姓名" align="center" />
        <el-table-column prop="phone" label="手机号" align="center" />
        <el-table-column prop="email" label="邮箱" align="center" />
        <el-table-column prop="role" label="角色" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center">
          <template #default="scope">
            <el-button
              v-if="scope.row.role === 'user'"
              type="warning"
              size="small"
              @click="openUpgradeDialog(scope.row)"
              class="upgrade-btn"
            >
              开通营业员
            </el-button>
            <el-button
              v-if="scope.row.role !== 'admin'"
              :type="scope.row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="toggleStatus(scope.row.id, scope.row.status === 1 ? 0 : 1)"
              class="status-btn"
            >
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              v-if="scope.row.role === 'clerk'"
              type="danger"
              size="small"
              @click="deleteClerk(scope.row.id)"
              class="delete-btn"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total">
        </el-pagination>
      </div>
    </el-card>
    
    <!-- 新增营业员对话框 -->
    <el-dialog title="新增营业员" v-model="clerkDialogVisible" width="450px" class="user-dialog">
      <el-form :model="clerkForm" :rules="clerkRules" ref="clerkFormRef" label-width="80px" class="dialog-form">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="clerkForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="clerkForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-alert
          title="账号和密码将在提交后自动生成"
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 10px"
        />
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="clerkDialogVisible = false" class="cancel-btn">取消</el-button>
          <el-button type="primary" @click="addClerk" class="save-btn">确认添加</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 开通营业员对话框 -->
    <el-dialog title="开通营业员权限" v-model="upgradeDialogVisible" width="400px" class="user-dialog">
      <div class="upgrade-content">
        <p>确定要为用户 <strong>{{ upgradeUser.realName || upgradeUser.username }}</strong> 开通营业员权限吗？</p>
        <p class="upgrade-tip">开通后该用户将获得营业员身份，可使用营业员相关功能。</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="upgradeDialogVisible = false" class="cancel-btn">取消</el-button>
          <el-button type="primary" @click="upgradeToClerk" class="save-btn">确认开通</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 添加成功对话框 -->
    <el-dialog title="营业员添加成功" v-model="successDialogVisible" width="400px" class="user-dialog">
      <div class="success-content">
        <p>账号：<strong>{{ generatedAccount.username }}</strong></p>
        <p>密码：<strong>{{ generatedAccount.password }}</strong></p>
        <el-alert
          title="请将此账号密码告知营业员本人，建议首次登录后修改密码"
          type="warning"
          :closable="false"
          show-icon
          style="margin-top: 15px"
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="successDialogVisible = false" class="save-btn">我知道了</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])

// 新增营业员对话框
const clerkDialogVisible = ref(false)
const clerkFormRef = ref(null)
const clerkForm = ref({
  realName: '',
  phone: ''
})
const clerkRules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 开通营业员对话框
const upgradeDialogVisible = ref(false)
const upgradeUser = ref({
  id: null,
  username: '',
  realName: ''
})

// 添加成功对话框
const successDialogVisible = ref(false)
const generatedAccount = ref({
  username: '',
  password: ''
})

const searchForm = ref({
  username: '',
  role: '',
  status: ''
})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(() => {
  fetchUsers()
})

const fetchUsers = async () => {
  try {
    let data = await userApi.list()
    
    if (searchForm.value.username) {
      data = data.filter(user => user.username.includes(searchForm.value.username))
    }
    if (searchForm.value.role) {
      data = data.filter(user => user.role === searchForm.value.role)
    }
    if (searchForm.value.status !== '') {
      data = data.filter(user => user.status === parseInt(searchForm.value.status))
    }
    
    total.value = data.length
    const startIndex = (currentPage.value - 1) * pageSize.value
    const endIndex = startIndex + pageSize.value
    users.value = data.slice(startIndex, endIndex)
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  }
}

const resetSearch = () => {
  searchForm.value = {
    username: '',
    role: '',
    status: ''
  }
  currentPage.value = 1
  fetchUsers()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchUsers()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  fetchUsers()
}

const toggleStatus = async (userId, status) => {
  try {
    await userApi.updateStatus(userId, status)
    ElMessage.success('状态更新成功')
    fetchUsers()
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
  }
}

// 新增营业员
const openAddClerkDialog = () => {
  clerkForm.value = {
    realName: '',
    phone: ''
  }
  clerkFormRef.value?.clearValidate()
  clerkDialogVisible.value = true
}

const addClerk = async () => {
  if (!clerkFormRef.value) return
  
  await clerkFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      const result = await userApi.addClerk({
        realName: clerkForm.value.realName,
        phone: clerkForm.value.phone
      })
      
      clerkDialogVisible.value = false
      generatedAccount.value = {
        username: result.username,
        password: result.password
      }
      successDialogVisible.value = true
      fetchUsers()
    } catch (error) {
      console.error('添加营业员失败:', error)
      ElMessage.error('添加营业员失败')
    }
  })
}

// 开通营业员权限
const openUpgradeDialog = (user) => {
  upgradeUser.value = {
    id: user.id,
    username: user.username,
    realName: user.realName || ''
  }
  upgradeDialogVisible.value = true
}

const upgradeToClerk = async () => {
  try {
    await userApi.updateRole(upgradeUser.value.id, 'clerk')
    ElMessage.success('开通成功，该用户已成为营业员')
    upgradeDialogVisible.value = false
    fetchUsers()
  } catch (error) {
    console.error('开通营业员失败:', error)
    ElMessage.error('开通营业员失败')
  }
}

// 删除营业员
const deleteClerk = async (userId) => {
  try {
    await ElMessageBox.confirm('确定要删除该营业员吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userApi.delete(userId)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除营业员失败:', error)
      ElMessage.error('删除营业员失败')
    }
  }
}
</script>

<style scoped>
.user-manage {
  background-color: transparent;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 30px;
}

.user-manage::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 30%, rgba(102, 126, 234, 0.08) 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(118, 75, 162, 0.08) 0%, transparent 40%);
  z-index: 0;
  animation: gradientShift 15s ease-in-out infinite;
}

@keyframes gradientShift {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.page-title {
  text-align: center;
  color: #333;
  font-size: 2rem;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
  animation: fadeIn 1s ease-out;
  flex-shrink: 0;
  margin-bottom: 30px;
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
  from { width: 0; }
  to { width: 100px; }
}

.search-card {
  margin-bottom: 20px;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  background: white;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.1);
  animation: fadeIn 0.8s ease-out both;
  animation-delay: 0.2s;
}

.page-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.add-btn {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.search-form {
  padding: 20px;
}

.search-btn {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.reset-btn {
  border-radius: 6px;
  transition: all 0.3s ease;
}

.reset-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.table-card {
  margin-bottom: 20px;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  background: white;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.1);
  animation: fadeIn 0.8s ease-out both;
  animation-delay: 0.4s;
}

.user-table {
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}

.user-table th {
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  font-weight: 600;
  color: #333;
}

.user-table tr:hover {
  background-color: rgba(102, 126, 234, 0.05);
  transition: background-color 0.3s ease;
}

.upgrade-btn {
  background: linear-gradient(90deg, #f6d365, #fda085);
  border: none;
  border-radius: 6px;
  transition: all 0.3s ease;
  margin-right: 8px;
}

.upgrade-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(253, 160, 133, 0.3);
}

.status-btn {
  border-radius: 6px;
  transition: all 0.3s ease;
}

.status-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.delete-btn {
  border-radius: 6px;
  transition: all 0.3s ease;
  margin-left: 8px;
}

.delete-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
  padding: 0 20px 20px;
}

.user-dialog {
  border-radius: 16px;
  overflow: hidden;
}

.user-dialog .el-dialog__header {
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  padding: 20px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
}

.user-dialog .el-dialog__title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
}

.dialog-form {
  padding: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 20px 20px;
}

.cancel-btn {
  border-radius: 6px;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.save-btn {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.save-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.upgrade-content {
  padding: 20px;
  text-align: center;
  font-size: 16px;
  line-height: 1.8;
}

.upgrade-tip {
  color: #666;
  font-size: 14px;
  margin-top: 10px;
}

.success-content {
  padding: 20px;
  text-align: center;
  font-size: 16px;
  line-height: 2;
}

.success-content strong {
  color: #667eea;
  font-size: 18px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .user-manage { padding: 20px; }
  .page-title { font-size: 1.8rem; }
  .search-form { padding: 15px; }
  .el-form-item {
    margin-bottom: 10px;
  }
  .el-form-item__label {
    width: 80px;
  }
  .el-form-item__content {
    margin-left: 90px !important;
  }
  .pagination {
    padding: 0 15px 15px;
  }
}
</style>