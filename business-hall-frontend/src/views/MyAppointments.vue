<template>
  <div class="my-appointments">
    <div class="container">
      <h1>我的预约</h1>
      
      <div v-if="appointments.length > 0">
        <el-table :data="appointments" style="width: 100%">
          <el-table-column prop="appointmentNo" label="预约编号" width="150" />
          <el-table-column prop="businessType" label="业务类型" />
          <el-table-column prop="appointmentDate" label="预约日期" width="120" />
          <el-table-column prop="appointmentTimeRange" label="预约时段" width="120" />
          <el-table-column prop="customerName" label="客户姓名" width="100" />
          <el-table-column prop="customerPhone" label="联系电话" width="130" />
          <el-table-column label="核验码" width="100">
            <template #default="scope">
              <span v-if="scope.row.status === '待办理'" class="verification-code-text">{{ scope.row.verificationCode }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="clerkName" label="办理营业员" width="110" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理结果" width="150">
            <template #default="scope">
              <span v-if="scope.row.result">{{ scope.row.result }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === '待办理'"
                type="primary"
                size="small"
                style="margin-right: 8px;"
                @click="showRescheduleDialog(scope.row)"
              >
                改签
              </el-button>
              <el-button
                v-if="scope.row.status === '待办理'"
                type="danger"
                size="small"
                @click="cancelAppointment(scope.row)"
              >
                取消
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <div v-else class="empty-state">
        <el-empty description="暂无预约记录">
          <el-button type="primary" @click="goToAppointment">去预约</el-button>
        </el-empty>
      </div>
    </div>
    
    <!-- 取消确认对话框 -->
    <el-dialog
      v-model="cancelDialogVisible"
      title="取消预约"
      width="400px"
    >
      <p class="cancel-confirm-text">确定要取消该预约吗？</p>
      <div class="appointment-detail">
        <p><strong>预约编号：</strong>{{ selectedAppointment?.appointmentNo }}</p>
        <p><strong>业务类型：</strong>{{ selectedAppointment?.businessType }}</p>
        <p><strong>预约日期：</strong>{{ selectedAppointment?.appointmentDate }}</p>
        <p><strong>预约时段：</strong>{{ selectedAppointment?.appointmentTimeRange }}</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelDialogVisible = false">返回</el-button>
          <el-button type="danger" @click="confirmCancel">确认取消</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 改签对话框 -->
    <el-dialog
      v-model="rescheduleDialogVisible"
      title="改签预约"
      width="600px"
      :close-on-click-modal="false"
    >
      <!-- 原预约信息 -->
      <div class="reschedule-original">
        <h3>原预约信息</h3>
        <div class="appointment-detail">
          <p><strong>预约编号：</strong>{{ rescheduleAppointment?.appointmentNo }}</p>
          <p><strong>业务类型：</strong>{{ rescheduleAppointment?.businessType }}</p>
          <p><strong>原预约日期：</strong>{{ rescheduleAppointment?.appointmentDate }}</p>
          <p><strong>原预约时段：</strong>{{ rescheduleAppointment?.appointmentTimeRange }}</p>
        </div>
      </div>

      <!-- 新预约信息表单 -->
      <div class="reschedule-form">
        <h3>选择新的预约时间</h3>
        
        <!-- 日期选择 -->
        <el-form-item label="新预约日期" prop="newDate" class="required">
          <el-date-picker
            v-model="rescheduleForm.newDate"
            type="date"
            placeholder="选择日期"
            :disabled-date="disabledRescheduleDate"
            style="width: 100%"
            @change="onRescheduleDateChange"
          />
        </el-form-item>

        <!-- 时段选择 -->
        <el-form-item label="新预约时段" prop="newTimeSlot" class="required">
          <div v-if="rescheduleTimeSlots.length > 0" class="time-slot-options">
            <el-button
              v-for="(slot, index) in rescheduleTimeSlots"
              :key="index"
              :type="rescheduleForm.newTimeSlotIndex === index ? 'primary' : 'default'"
              :disabled="slot.bookedCount >= slot.maxQuota"
              @click="selectRescheduleTimeSlot(index)"
              size="small"
            >
              {{ slot.startTime }} - {{ slot.endTime }} (剩余: {{ slot.maxQuota - slot.bookedCount }})
            </el-button>
          </div>
          <div v-else-if="rescheduleForm.newDate" class="no-time-slots">
            <p>该日期暂无可预约时段</p>
          </div>
          <div v-else class="no-time-slots">
            <p>请先选择预约日期</p>
          </div>
        </el-form-item>
      </div>

      <!-- 加载状态提示 -->
      <div v-if="rescheduleLoading" class="loading-overlay">
        <el-loading-spinner />
        <span>提交中...</span>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeRescheduleDialog">返回</el-button>
          <el-button
            type="primary"
            :disabled="!canSubmitReschedule || rescheduleLoading"
            @click="confirmReschedule"
          >
            {{ rescheduleLoading ? '提交中...' : '确认改签' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { appointmentApi, resourceApi, timeslotApi } from '../api'
import { useUserStore } from '../store/user'
import { formatDate } from '../utils/dateUtil'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const appointments = ref([])

// 取消预约相关变量
const cancelDialogVisible = ref(false)
const selectedAppointment = ref(null)

// 改签相关变量
const rescheduleDialogVisible = ref(false)
const rescheduleAppointment = ref(null)
const rescheduleLoading = ref(false)
const rescheduleTimeSlots = ref([])
const rescheduleForm = ref({
  newDate: null,
  newTimeSlotIndex: null
})

onMounted(() => {
  fetchAppointments()
})

const fetchAppointments = async () => {
  try {
    appointments.value = await appointmentApi.getMyList(userStore.user.id)
  } catch (error) {
    console.error('获取预约记录失败:', error)
    ElMessage.error('获取预约记录失败')
  }
}

// 获取状态标签类型
const getStatusType = (status) => {
  switch (status) {
    case '待办理':
      return 'warning'
    case '办理中':
      return 'primary'
    case '已完成':
      return 'success'
    case '已取消':
      return 'info'
    case '爽约':
      return 'danger'
    default:
      return ''
  }
}

// 取消预约
const cancelAppointment = (appointment) => {
  selectedAppointment.value = appointment
  cancelDialogVisible.value = true
}

// 确认取消
const confirmCancel = async () => {
  if (!selectedAppointment.value) return
  
  try {
    await appointmentApi.cancel(selectedAppointment.value.id)
    ElMessage.success('取消成功')
    cancelDialogVisible.value = false
    fetchAppointments()
  } catch (error) {
    ElMessage.error(error.response?.data || '取消失败')
  }
}

// 显示改签对话框
const showRescheduleDialog = (appointment) => {
  rescheduleAppointment.value = appointment
  rescheduleForm.value = {
    newDate: null,
    newTimeSlotIndex: null
  }
  rescheduleTimeSlots.value = []
  rescheduleDialogVisible.value = true
}

// 关闭改签对话框
const closeRescheduleDialog = () => {
  rescheduleDialogVisible.value = false
  rescheduleAppointment.value = null
  rescheduleForm.value = {
    newDate: null,
    newTimeSlotIndex: null
  }
  rescheduleTimeSlots.value = []
}

// 改签日期选择器禁用函数 - 禁用过去日期和周休日
const disabledRescheduleDate = (date) => {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  // 禁用过去日期
  if (date < now) return true
  
  // 检查是否为休息日（周日默认不可预约）
  const dayOfWeek = date.getDay()
  if (dayOfWeek === 0) return true
  
  return false
}

// 改签日期变更时获取可用时段
const onRescheduleDateChange = async (date) => {
  if (!date || !rescheduleAppointment.value) return
  
  const formattedDate = formatDate(date)
  try {
    // 获取该日期的可用时段信息
    const resourceRes = await resourceApi.getByDate(formattedDate)
    const resourceData = resourceRes.data || {}
    
    if (!resourceData.available) {
      ElMessage.warning(resourceData.message || '该日期不可预约')
      rescheduleTimeSlots.value = []
      rescheduleForm.value.newTimeSlotIndex = null
      return
    }
    
    // 检查所选业务类型在该日期是否可用
    const availableTypes = resourceData.availableBusinessTypes || []
    if (!availableTypes.includes(rescheduleAppointment.value.businessType)) {
      ElMessage.warning(`该日期暂不支持"${rescheduleAppointment.value.businessType}"业务，请选择其他日期`)
      rescheduleTimeSlots.value = []
      rescheduleForm.value.newTimeSlotIndex = null
      return
    }
    
    // 直接使用resource返回的时段数据（使用startTime和endTime作为改签参数）
    const availableSlots = resourceData.timeSlots || []
    rescheduleTimeSlots.value = availableSlots.map((slot) => ({
      ...slot,
      bookedCount: slot.bookedCount || 0
    }))
    
    rescheduleForm.value.newTimeSlotIndex = null
  } catch (error) {
    console.error('获取可用时段失败:', error)
    rescheduleTimeSlots.value = []
    ElMessage.error('获取时段信息失败')
  }
}

// 选择改签时段
const selectRescheduleTimeSlot = (index) => {
  rescheduleForm.value.newTimeSlotIndex = index
}

// 判断是否可以提交改签
const canSubmitReschedule = computed(() => {
  return rescheduleForm.value.newDate !== null && 
         rescheduleForm.value.newTimeSlotIndex !== null &&
         rescheduleTimeSlots.value.length > 0
})

// 确认改签
const confirmReschedule = async () => {
  if (!canSubmitReschedule.value || !rescheduleAppointment.value) return
  
  const selectedSlot = rescheduleTimeSlots.value[rescheduleForm.value.newTimeSlotIndex]
  if (!selectedSlot) {
    ElMessage.error('请选择有效的时段')
    return
  }
  
  // 获取时段时间信息
  const { startTime, endTime } = selectedSlot
  if (!startTime || !endTime) {
    ElMessage.error('时段信息不完整，无法完成改签')
    return
  }
  
  // 获取新预约日期
  const newDate = formatDate(rescheduleForm.value.newDate)
  if (!newDate) {
    ElMessage.error('请选择预约日期')
    return
  }
  
  rescheduleLoading.value = true
  
  try {
    // 调用改签API（使用日期和时段时间信息）
    await appointmentApi.reschedule(
      rescheduleAppointment.value.id,
      newDate,
      startTime,
      endTime
    )
    
    ElMessage.success('改签成功')
    closeRescheduleDialog()
    fetchAppointments() // 刷新预约列表
  } catch (error) {
    console.error('改签失败:', error)
    ElMessage.error(error.response?.data?.message || error.response?.data || '改签失败')
  } finally {
    rescheduleLoading.value = false
  }
}

// 跳转到预约页面
const goToAppointment = () => {
  router.push('/appointment')
}
</script>

<style scoped>
.my-appointments {
  background-color: white;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.my-appointments::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 10% 10%, rgba(102, 126, 234, 0.05) 0%, transparent 30%),
    radial-gradient(circle at 90% 90%, rgba(118, 75, 162, 0.05) 0%, transparent 30%);
  z-index: 0;
}

.container {
  width: 100%;
  padding: 0 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}

.my-appointments h1 {
  text-align: center;
  margin: 30px 0;
  color: #333;
  font-size: 2rem;
  font-weight: 700;
  position: relative;
  z-index: 1;
  animation: fadeIn 1s ease-out;
}

.my-appointments h1::after {
  content: '';
  display: block;
  width: 80px;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  margin: 15px auto 0;
  border-radius: 2px;
  animation: expand 1.5s ease-out 0.5s both;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes expand {
  from {
    width: 0;
  }
  to {
    width: 80px;
  }
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background-color: #f9f9fa;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  margin: 30px 0;
  position: relative;
  z-index: 1;
  animation: fadeIn 0.8s ease-out;
}

.el-table {
  margin: 30px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
  animation: fadeIn 0.8s ease-out;
}

.el-table__header-wrapper th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
}

.el-table__body-wrapper tr {
  transition: all 0.3s ease;
}

.el-table__body-wrapper tr:hover {
  background-color: rgba(102, 126, 234, 0.05);
}

.el-table__body-wrapper tr td {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.el-tag {
  border-radius: 4px;
}

.el-button {
  border-radius: 6px;
  transition: all 0.3s ease;
  padding: 6px 15px;
  font-size: 0.9rem;
}

.el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.el-button--primary {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
}

.el-button--primary:hover {
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.el-button--danger {
  background: linear-gradient(90deg, #f56565, #c53030);
  border: none;
}

.el-button--danger:hover {
  box-shadow: 0 6px 16px rgba(245, 101, 101, 0.4);
}

.cancel-confirm-text {
  font-size: 1.1rem;
  color: #333;
  margin-bottom: 20px;
  text-align: center;
}

.appointment-detail {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-top: 10px;
}

.appointment-detail p {
  margin: 8px 0;
  color: #666;
}

.appointment-detail strong {
  color: #333;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.dialog-footer .el-button {
  border-radius: 6px;
  transition: all 0.3s ease;
}

.verification-code-text {
  color: #e65100;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  font-size: 1rem;
  letter-spacing: 2px;
}

.text-muted {
  color: #c0c4cc;
}

/* 改签对话框样式 */
.reschedule-original {
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.reschedule-original h3,
.reschedule-form h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
  padding-left: 10px;
  border-left: 3px solid #667eea;
}

.reschedule-form {
  background: #f9f9fa;
  padding: 20px;
  border-radius: 8px;
}

.reschedule-form .required .el-form-item__label::after {
  content: '*';
  color: #f56565;
  margin-left: 4px;
}

.time-slot-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.time-slot-options .el-button {
  border-radius: 6px;
  padding: 8px 16px;
  transition: all 0.3s ease;
}

.time-slot-options .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.time-slot-options .el-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.no-time-slots {
  text-align: center;
  padding: 30px 20px;
  background: white;
  border-radius: 8px;
  color: #999;
  margin-top: 10px;
}

.no-time-slots p {
  margin: 0;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  z-index: 100;
  border-radius: 4px;
}

.loading-overlay .el-loading-spinner {
  width: 40px;
  height: 40px;
}

.loading-overlay span {
  color: #667eea;
  font-weight: 500;
}

@media (max-width: 1200px) {
  .container {
    width: 100%;
  }
}
</style>