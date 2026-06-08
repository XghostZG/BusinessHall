<template>
  <div class="appointment-manage">
    <!-- 统计信息看板 -->
    <div class="stats-container">
      <div class="stat-card">
        <div class="stat-icon"><el-icon><i class="el-icon-s-order"></i></el-icon></div>
        <div class="stat-number">{{ totalAppointments }}</div>
        <div class="stat-label">今日预约总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><el-icon><i class="el-icon-time"></i></el-icon></div>
        <div class="stat-number">{{ pendingAppointments }}</div>
        <div class="stat-label">待办理</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><el-icon><i class="el-icon-s-processing"></i></el-icon></div>
        <div class="stat-number">{{ processingAppointments }}</div>
        <div class="stat-label">办理中</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><el-icon><i class="el-icon-check"></i></el-icon></div>
        <div class="stat-number">{{ completedAppointments }}</div>
        <div class="stat-label">已完成</div>
      </div>
    </div>
    
    <!-- 筛选和搜索区域 -->
    <div class="filter-container">
      <el-select v-model="statusFilter" placeholder="按状态筛选" class="filter-select">
        <el-option label="全部" value="" />
        <el-option label="待办理" value="待办理" />
        <el-option label="办理中" value="办理中" />
        <el-option label="已完成" value="已完成" />
        <el-option label="爽约" value="爽约" />
      </el-select>
      <el-input v-model="searchKeyword" placeholder="搜索客户姓名或手机号" class="search-input">
        <template #append>
          <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
        </template>
      </el-input>
    </div>
    
    <!-- 预约列表 -->
    <div class="appointment-list">
      <div class="list-header">
        <h2 class="list-title">今日预约</h2>
        <span class="list-subtitle">{{ filteredAppointments.length }} 条记录</span>
      </div>
      <el-table :data="filteredAppointments" style="width: 100%" v-loading="loading" class="appointment-table">
        <el-table-column prop="id" label="预约编号" width="100" />
        <el-table-column prop="customerName" label="客户姓名" />
        <el-table-column prop="customerPhone" label="手机号" />
        <el-table-column prop="businessType" label="业务类型" />
        <el-table-column prop="appointmentTimeRange" label="预约时段" />
        <el-table-column prop="clerkName" label="办理营业员" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" class="status-tag">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理结果" width="150">
          <template #default="scope">
            <span v-if="scope.row.result">{{ scope.row.result }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === '待办理'"
              type="primary"
              size="small"
              @click="openVerifyDialog(scope.row)"
              class="action-button primary-button"
            >
              开始办理
            </el-button>
            <el-button
              v-if="scope.row.status === '办理中'"
              type="success"
              size="small"
              @click="openResultDialog(scope.row)"
              class="action-button success-button"
            >
              完成办理
            </el-button>
            <el-button
              v-if="scope.row.status === '待办理'"
              type="danger"
              size="small"
              @click="markNoShow(scope.row.id)"
              class="action-button danger-button"
            >
              爽约
            </el-button>
            <el-button
              type="info"
              size="small"
              @click="viewAppointmentDetail(scope.row)"
              class="action-button info-button"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 空状态 -->
      <div v-if="!loading && filteredAppointments.length === 0" class="empty-state">
        <el-empty description="暂无预约数据" />
      </div>
    </div>
    
    <!-- 核验码验证对话框 -->
    <el-dialog
      v-model="verifyDialogVisible"
      title="验证核验码"
      width="400px"
      class="appointment-dialog"
      :close-on-click-modal="false"
    >
      <div class="verify-dialog-content">
        <p class="verify-tip">请输入用户出示的6位核验码</p>
        <el-input
          v-model="inputVerificationCode"
          placeholder="请输入6位核验码"
          maxlength="6"
          size="large"
          class="verify-input"
          @keyup.enter="confirmVerify"
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="verifyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmVerify" :loading="verifyLoading">验证并开始办理</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 处理结果对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      title="输入处理结果"
      width="500px"
      class="appointment-dialog"
      :close-on-click-modal="false"
    >
      <div class="result-dialog-content">
        <p class="result-tip">请输入本次业务的处理结果</p>
        <el-input
          v-model="inputResult"
          type="textarea"
          :rows="4"
          placeholder="请输入处理结果，如：开户成功、业务变更完成等"
          maxlength="500"
          show-word-limit
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resultDialogVisible = false">取消</el-button>
          <el-button type="success" @click="confirmComplete" :loading="resultLoading">确认完成</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 预约详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="预约详情"
      width="500px"
      class="appointment-dialog"
    >
      <div v-if="selectedAppointment" class="appointment-detail">
        <el-descriptions :column="1" border class="detail-descriptions">
          <el-descriptions-item label="预约编号">{{ selectedAppointment.id }}</el-descriptions-item>
          <el-descriptions-item label="客户姓名">{{ selectedAppointment.customerName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ selectedAppointment.customerPhone }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ selectedAppointment.businessType }}</el-descriptions-item>
          <el-descriptions-item label="预约时段">{{ selectedAppointment.appointmentTimeRange }}</el-descriptions-item>
          <el-descriptions-item label="核验码">{{ selectedAppointment.verificationCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="办理营业员">{{ selectedAppointment.clerkName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ selectedAppointment.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedAppointment.status)">
              {{ selectedAppointment.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedAppointment.result" label="处理结果">{{ selectedAppointment.result }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" class="close-button">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { appointmentApi } from '../../api'
import { useUserStore } from '../../store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const userStore = useUserStore()
const appointments = ref([])
const loading = ref(false)
const statusFilter = ref('')
const searchKeyword = ref('')
const dialogVisible = ref(false)
const selectedAppointment = ref(null)

// 核验码验证相关
const verifyDialogVisible = ref(false)
const inputVerificationCode = ref('')
const verifyLoading = ref(false)
const currentVerifyAppointment = ref(null)

// 处理结果相关
const resultDialogVisible = ref(false)
const inputResult = ref('')
const resultLoading = ref(false)
const currentResultAppointment = ref(null)

onMounted(() => {
  fetchTodayAppointments()
})

const fetchTodayAppointments = () => {
  loading.value = true
  appointmentApi.getToday(userStore.user.id)
    .then(data => {
      appointments.value = data || []
    })
    .catch(error => {
      console.error('获取今日预约失败:', error)
      ElMessage.error('获取今日预约失败')
    })
    .finally(() => {
      loading.value = false
    })
}

// 打开核验码验证对话框
const openVerifyDialog = (appointment) => {
  currentVerifyAppointment.value = appointment
  inputVerificationCode.value = ''
  verifyDialogVisible.value = true
}

// 确认核验码验证
const confirmVerify = async () => {
  if (!inputVerificationCode.value) {
    ElMessage.warning('请输入核验码')
    return
  }
  if (inputVerificationCode.value.length !== 6) {
    ElMessage.warning('核验码为6位数字')
    return
  }
  
  verifyLoading.value = true
  try {
    await appointmentApi.startWithVerification(
      currentVerifyAppointment.value.id,
      userStore.user.id,
      inputVerificationCode.value
    )
    ElMessage.success('核验通过，开始办理')
    verifyDialogVisible.value = false
    fetchTodayAppointments()
  } catch (error) {
    ElMessage.error(error.response?.data || '核验码错误，请重新输入')
  } finally {
    verifyLoading.value = false
  }
}

// 打开处理结果对话框
const openResultDialog = (appointment) => {
  currentResultAppointment.value = appointment
  inputResult.value = ''
  resultDialogVisible.value = true
}

// 确认完成办理
const confirmComplete = async () => {
  if (!inputResult.value.trim()) {
    ElMessage.warning('请输入处理结果')
    return
  }
  
  resultLoading.value = true
  try {
    await appointmentApi.completeWithResult(
      currentResultAppointment.value.id,
      userStore.user.id,
      inputResult.value.trim()
    )
    ElMessage.success('办理完成')
    resultDialogVisible.value = false
    fetchTodayAppointments()
  } catch (error) {
    ElMessage.error(error.response?.data || '完成办理失败')
  } finally {
    resultLoading.value = false
  }
}

const markNoShow = (appointmentId) => {
  ElMessageBox.confirm('确定要标记该预约为爽约吗？', '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    loading.value = true
    appointmentApi.updateStatus(appointmentId, '爽约')
      .then(() => {
        ElMessage.success('已标记为爽约')
        fetchTodayAppointments()
      })
      .catch(error => {
        ElMessage.error('操作失败：' + (error.response?.data || ''))
      })
      .finally(() => {
        loading.value = false
      })
  }).catch(() => {})
}

const viewAppointmentDetail = (appointment) => {
  selectedAppointment.value = appointment
  dialogVisible.value = true
}

const handleSearch = () => {
  // 搜索逻辑已在computed属性中处理
}

const getStatusType = (status) => {
  const statusMap = {
    '待办理': 'info',
    '办理中': 'primary',
    '已完成': 'success',
    '爽约': 'danger',
    '已取消': 'info'
  }
  return statusMap[status] || 'info'
}

// 计算属性：筛选后的预约列表
const filteredAppointments = computed(() => {
  return appointments.value.filter(appointment => {
    // 状态筛选
    if (statusFilter.value && appointment.status !== statusFilter.value) {
      return false
    }
    // 关键词搜索
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      return (
        appointment.customerName.toLowerCase().includes(keyword) ||
        appointment.customerPhone.includes(keyword)
      )
    }
    return true
  })
})

// 计算属性：统计数据
const totalAppointments = computed(() => {
  return appointments.value.length
})

const pendingAppointments = computed(() => {
  return appointments.value.filter(a => a.status === '待办理').length
})

const processingAppointments = computed(() => {
  return appointments.value.filter(a => a.status === '办理中').length
})

const completedAppointments = computed(() => {
  return appointments.value.filter(a => a.status === '已完成').length
})
</script>

<style scoped>
.appointment-manage {
  padding: 20px;
}

/* 统计信息看板 */
.stats-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  text-align: center;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
}

.stat-icon {
  font-size: 2rem;
  color: #667eea;
  margin-bottom: 16px;
  text-shadow: 0 2px 4px rgba(102, 126, 234, 0.3);
}

.stat-number {
  font-size: 32px;
  font-weight: 600;
  color: #667eea;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

/* 筛选和搜索区域 */
.filter-container {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  align-items: center;
}

.filter-select {
  width: 160px;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

/* 预约列表 */
.appointment-list {
  background-color: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list-title {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.list-subtitle {
  font-size: 14px;
  color: #667eea;
  font-weight: 600;
}

.appointment-table {
  border-radius: 8px;
  overflow: hidden;
}

/* 状态标签 */
.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 核验码样式 */
.verification-code-text {
  color: #e65100;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  font-size: 0.95rem;
  letter-spacing: 2px;
}

/* 操作按钮 */
.action-button {
  border-radius: 6px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.action-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.primary-button {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
}

.success-button {
  background: linear-gradient(90deg, #4CAF50, #45a049);
  border: none;
}

.warning-button {
  background: linear-gradient(90deg, #ff9800, #f57c00);
  border: none;
}

.danger-button {
  background: linear-gradient(90deg, #f44336, #da190b);
  border: none;
}

.info-button {
  background: linear-gradient(90deg, #2196F3, #0b7dda);
  border: none;
}

/* 空状态 */
.empty-state {
  padding: 40px 0;
  text-align: center;
}

/* 预约详情 */
.appointment-detail {
  padding: 10px 0;
}

.detail-descriptions {
  border-radius: 8px;
  overflow: hidden;
}

/* 对话框 */
.appointment-dialog {
  border-radius: 12px;
  overflow: hidden;
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

/* 核验码验证对话框 */
.verify-dialog-content,
.result-dialog-content {
  padding: 10px 0;
}

.verify-tip,
.result-tip {
  color: #666;
  margin-bottom: 16px;
  font-size: 0.95rem;
}

.verify-input :deep(.el-input__inner) {
  font-size: 1.5rem;
  letter-spacing: 8px;
  text-align: center;
  font-weight: 700;
  color: #e65100;
}

/* 操作按钮间距 */
::v-deep(.el-button + .el-button) {
  margin-left: 8px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .stats-container {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-container {
    grid-template-columns: 1fr;
  }
  
  .filter-container {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-select,
  .search-input {
    width: 100%;
    max-width: none;
  }
  
  .list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
