<template>
  <div class="appointment">
    <div class="container">
      <h1>业务预约</h1>
      
      <el-steps :active="activeStep" finish-status="success">
        <el-step title="选择业务类型" />
        <el-step title="选择日期" />
        <el-step title="选择时段" />
        <el-step title="填写信息" />
      </el-steps>
      
      <div class="step-content">
        <!-- 步骤1：选择业务类型 -->
        <div v-if="activeStep === 0">
          <h2>请选择业务类型</h2>
          <div class="business-list">
            <el-card
              v-for="businessType in businessTypes"
              :key="businessType"
              :body-style="{ padding: '15px' }"
              @click="selectBusiness(businessType)"
              :class="{ 'selected': selectedBusinessType === businessType }"
            >
              <h3>{{ businessType }}</h3>
            </el-card>
          </div>
        </div>
        
        <!-- 步骤2：选择日期 -->
        <div v-if="activeStep === 1">
          <h2>请选择预约日期</h2>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            :disabled-date="disabledDate"
            style="width: 100%"
            @change="onDateChange"
          />
          <div class="step-actions">
            <el-button @click="prevStep">上一步</el-button>
            <el-button type="primary" :disabled="!selectedDate" @click="nextStep">下一步</el-button>
          </div>
        </div>
        
        <!-- 步骤3：选择时段 -->
        <div v-if="activeStep === 2">
          <h2>请选择预约时段</h2>
          <div v-if="timeSlots.length > 0" class="time-slot-list">
            <el-button
              v-for="(slot, index) in timeSlots"
              :key="index"
              :type="selectedSlotIndex === index ? 'primary' : 'default'"
              :disabled="slot.bookedCount >= slot.maxQuota"
              @click="selectTimeSlot(index)"
            >
              {{ slot.startTime }} - {{ slot.endTime }} (剩余: {{ slot.maxQuota - slot.bookedCount }})
            </el-button>
          </div>
          <div v-else class="no-data">
            <p>该日期暂无可预约时段</p>
          </div>
          <div class="step-actions">
            <el-button @click="prevStep">上一步</el-button>
            <el-button type="primary" :disabled="selectedSlotIndex === null" @click="nextStep">下一步</el-button>
          </div>
        </div>
        
        <!-- 步骤4：填写信息 -->
        <div v-if="activeStep === 3">
          <h2>请填写预约信息</h2>
          <div class="appointment-summary">
            <div class="summary-item">
              <span class="label">业务类型：</span>
              <span class="value">{{ selectedBusinessType }}</span>
            </div>
            <div class="summary-item">
              <span class="label">预约日期：</span>
              <span class="value">{{ formatDateDisplay(selectedDate) }}</span>
            </div>
            <div class="summary-item">
              <span class="label">预约时段：</span>
              <span class="value">{{ selectedTimeSlot ? selectedTimeSlot.startTime + ' - ' + selectedTimeSlot.endTime : '' }}</span>
            </div>
          </div>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
            <el-form-item label="姓名" prop="customerName">
              <el-input v-model="form.customerName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="customerPhone">
              <el-input v-model="form.customerPhone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input type="textarea" v-model="form.remark" placeholder="请输入备注信息" />
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <el-button @click="prevStep">上一步</el-button>
            <el-button type="primary" @click="submitAppointment">提交预约</el-button>
          </div>
        </div>
        
        <!-- 预约成功 -->
        <div v-if="activeStep === 4" class="success-container">
          <div class="success-icon">
            <el-icon :size="80"><Check /></el-icon>
          </div>
          <h2>预约成功！</h2>
          <p class="appointment-no">预约号：{{ appointmentNo }}</p>
          <div class="verification-code-box" v-if="verificationCode">
            <div class="verification-label">请记住您的核验码，办理时需出示</div>
            <p class="verification-code">{{ verificationCode }}</p>
          </div>
          <div class="success-details">
            <p>业务类型：{{ selectedBusinessType }}</p>
            <p>预约日期：{{ formatDateDisplay(selectedDate) }}</p>
            <p>预约时段：{{ selectedTimeSlot ? selectedTimeSlot.startTime + ' - ' + selectedTimeSlot.endTime : '' }}</p>
          </div>
          <div class="step-actions">
            <el-button type="primary" @click="goToMyAppointments">查看我的预约</el-button>
            <el-button @click="resetAndBookAgain">继续预约</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { appointmentApi, resourceApi } from '../api'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { formatDate } from '../utils/dateUtil'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeStep = ref(0)
const businessTypes = ref([])
const selectedBusinessType = ref(route.query.businessType || null)
const selectedDate = ref(null)
const timeSlots = ref([])
const selectedSlotIndex = ref(null)
const formRef = ref(null)
const appointmentNo = ref('')
const verificationCode = ref('')
const form = ref({
  customerName: '',
  customerPhone: '',
  remark: ''
})

// 计算当前选中的时段
const selectedTimeSlot = computed(() => {
  if (selectedSlotIndex.value !== null && timeSlots.value[selectedSlotIndex.value]) {
    return timeSlots.value[selectedSlotIndex.value]
  }
  return null
})

// 表单校验规则
const rules = {
  customerName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  customerPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

onMounted(async () => {
  try {
    const res = await resourceApi.getBusinessTypes()
    businessTypes.value = res.data || []
    
    // 初始化表单，默认填充当前用户信息
    if (userStore.user) {
      form.value.customerName = userStore.user.realName || ''
      form.value.customerPhone = userStore.user.phone || ''
    }
  } catch (error) {
    console.error('获取业务类型失败:', error)
  }
})

// 日期选择器禁用函数 - 禁用过去日期和周休日
const disabledDate = (date) => {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  // 禁用过去日期
  if (date < now) return true
  
  // 检查是否为休息日（通过获取可用信息判断）
  const dayOfWeek = date.getDay()
  // 周日默认不可预约
  if (dayOfWeek === 0) return true
  
  return false
}

// 选择业务类型
const selectBusiness = (businessType) => {
  selectedBusinessType.value = businessType
  nextStep()
}

// 日期变更时获取可用时段
const onDateChange = async (date) => {
  if (date) {
    const formattedDate = formatDate(date)
    try {
      const res = await resourceApi.getByDate(formattedDate)
      const data = res.data || {}
      
      if (!data.available) {
        ElMessage.warning(data.message || '该日期不可预约')
        timeSlots.value = []
        return
      }
      
      // 检查所选业务类型在该日期是否可用
      const availableTypes = data.availableBusinessTypes || []
      if (!availableTypes.includes(selectedBusinessType.value)) {
        ElMessage.warning(`该日期暂不支持"${selectedBusinessType.value}"业务，请选择其他日期或业务类型`)
        timeSlots.value = []
        return
      }
      
      // 解析时段数据（后端已计算好已预约数量）
      const slots = data.timeSlots || []
      timeSlots.value = slots.map((slot) => ({
        ...slot,
        bookedCount: slot.bookedCount || 0
      }))
      
      selectedSlotIndex.value = null
    } catch (error) {
      console.error('获取可用时段失败:', error)
      timeSlots.value = []
    }
  }
}

// 选择时段
const selectTimeSlot = (index) => {
  selectedSlotIndex.value = index
}

// 格式化日期显示
const formatDateDisplay = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

const nextStep = () => {
  activeStep.value++
}

const prevStep = () => {
  activeStep.value--
}

// 提交预约
const submitAppointment = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 检查是否选择了时段
  if (!selectedTimeSlot.value) {
    ElMessage.warning('请选择预约时段')
    return
  }

  const appointmentData = {
    userId: userStore.user.id,
    businessType: selectedBusinessType.value,
    appointmentDate: formatDate(selectedDate.value),
    timeSlotJson: JSON.stringify({
      startTime: selectedTimeSlot.value.startTime,
      endTime: selectedTimeSlot.value.endTime
    }),
    appointmentTimeRange: `${selectedTimeSlot.value.startTime}-${selectedTimeSlot.value.endTime}`,
    customerName: form.value.customerName,
    customerPhone: form.value.customerPhone,
    remark: form.value.remark
  }

  try {
    const result = await appointmentApi.create(appointmentData)
    appointmentNo.value = result.appointmentNo || ''
    verificationCode.value = result.verificationCode || ''
    ElMessage.success('预约成功！')
    activeStep.value = 4 // 显示成功页面
  } catch (error) {
    ElMessage.error(error.response?.data || error.message || '预约失败')
  }
}

// 跳转到我的预约
const goToMyAppointments = () => {
  router.push('/my-appointments')
}

// 重置并继续预约
const resetAndBookAgain = () => {
  activeStep.value = 0
  selectedBusinessType.value = null
  selectedDate.value = null
  timeSlots.value = []
  selectedSlotIndex.value = null
  
  // 重置表单时重新填充当前用户信息
  form.value = {
    customerName: userStore.user?.realName || '',
    customerPhone: userStore.user?.phone || '',
    remark: ''
  }
  
  appointmentNo.value = ''
  verificationCode.value = ''
}
</script>

<style scoped>
.appointment {
  background-color: white;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.appointment::before {
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

.appointment h1 {
  text-align: center;
  margin: 30px 0;
  color: #333;
  font-size: 2rem;
  font-weight: 700;
  position: relative;
  z-index: 1;
  animation: fadeIn 1s ease-out;
}

.appointment h1::after {
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

.step-content {
  margin-top: 30px;
  padding: 30px;
  background-color: #f9f9fa;
  border-radius: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
  animation: fadeIn 0.8s ease-out;
}

.step-content h2 {
  margin-bottom: 30px;
  color: #333;
  font-size: 1.3rem;
  font-weight: 600;
  position: relative;
  padding-left: 15px;
}

.step-content h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: linear-gradient(180deg, #667eea, #764ba2);
  border-radius: 2px;
}

.business-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 50px;
  padding: 0 10px;
}

.business-list .el-card {
  width: 100%;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 20px;
  overflow: hidden;
  border: 2px solid transparent;
  background: linear-gradient(white, white) padding-box, linear-gradient(135deg, #667eea, #764ba2) border-box;
  position: relative;
}

.business-list .el-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transform: scaleX(0);
  transition: transform 0.4s ease;
}

.business-list .el-card:hover {
  transform: translateY(-12px) scale(1.02);
  box-shadow: 0 16px 32px rgba(102, 126, 234, 0.25);
  border-color: #667eea;
}

.business-list .el-card:hover::before {
  transform: scaleX(1);
}

.business-list .el-card.selected {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.2);
  background: linear-gradient(rgba(102, 126, 234, 0.05), rgba(102, 126, 234, 0.05)) padding-box, linear-gradient(135deg, #667eea, #764ba2) border-box;
}

.business-list .el-card h3 {
  font-size: 1.2rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 16px;
  transition: color 0.3s ease, transform 0.3s ease;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
  display: flex;
  align-items: center;
  gap: 10px;
}

.business-list .el-card h3::before {
  content: '📋';
  font-size: 1.4rem;
}

.business-list .el-card:hover h3 {
  color: #667eea;
  transform: translateX(5px);
}

.time-slot-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 30px;
}

.time-slot-list .el-button {
  margin-bottom: 10px;
  border-radius: 8px;
  padding: 10px 20px;
  transition: all 0.3s ease;
}

.time-slot-list .el-button:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.time-slot-list .el-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.no-data {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin: 20px 0;
}

.no-data p {
  font-size: 1.1rem;
  margin: 0;
}

.appointment-summary {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  border: 1px solid #eee;
}

.summary-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-item .label {
  color: #666;
  width: 100px;
  font-weight: 500;
}

.summary-item .value {
  color: #333;
  font-weight: 600;
}

.step-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.step-actions .el-button {
  border-radius: 8px;
  padding: 10px 25px;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.step-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.step-actions .el-button--primary {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
}

.step-actions .el-button--primary:hover {
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.el-steps {
  margin: 30px 0;
  position: relative;
  z-index: 1;
}

.el-step__title {
  font-size: 1rem;
  font-weight: 500;
}

.el-step.is-success .el-step__title {
  color: #667eea;
}

.el-step.is-active .el-step__title {
  color: #667eea;
  font-weight: 600;
}

.el-step__icon.is-success {
  background-color: #667eea;
  border-color: #667eea;
}

.el-step__icon.is-active {
  background-color: #667eea;
  border-color: #667eea;
}

.el-form {
  margin-bottom: 20px;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-input,
.el-textarea {
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.el-input:focus,
.el-textarea:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.el-date-picker {
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.el-date-picker:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.success-container {
  text-align: center;
  padding: 40px 20px;
}

.success-icon {
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 30px;
  color: white;
}

.success-container h2 {
  color: #333;
  margin-bottom: 20px;
  font-size: 1.5rem;
}

.appointment-no {
  font-size: 1.5rem;
  color: #667eea;
  font-weight: 700;
  margin-bottom: 20px;
}

.verification-code-box {
  background: linear-gradient(135deg, #fff3e0, #ffe0b2);
  border: 2px dashed #ff9800;
  border-radius: 12px;
  padding: 20px 30px;
  margin: 0 auto 30px;
  display: inline-block;
}

.verification-label {
  color: #e65100;
  font-size: 0.9rem;
  margin-bottom: 8px;
  font-weight: 500;
}

.verification-code {
  font-size: 2.5rem;
  font-weight: 800;
  color: #e65100;
  letter-spacing: 12px;
  margin: 0;
  font-family: 'Courier New', monospace;
}

.success-details {
  background: #f9f9fa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  text-align: left;
  display: inline-block;
  min-width: 300px;
}

.success-details p {
  margin: 10px 0;
  color: #666;
}

@media (max-width: 1200px) {
  .container {
    width: 100%;
  }
  
  .business-list {
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .business-list {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 0 5px;
  }
  
  .business-list .el-card {
    border-radius: 16px;
  }
  
  .business-list .el-card h3 {
    font-size: 1.1rem;
    margin-bottom: 12px;
  }
}
</style>