<template>
  <div class="page-container">
    <!-- 每日打卡区域 -->
    <div class="section-title">
      <el-icon><Clock /></el-icon>
      <span>每日打卡</span>
    </div>
    <el-card class="checkin-card" shadow="hover">
      <div class="work-time-info">
        <div class="time-item">
          <el-icon><Sunrise /></el-icon>
          <span>上班时间: 09:00</span>
        </div>
        <div class="time-item">
          <el-icon><Sunset /></el-icon>
          <span>下班时间: 18:00</span>
        </div>
      </div>
      <div class="current-status">
        <div class="date-display">
          <span class="weekday">{{ weekDay }}</span>
          <span class="date">{{ currentDateStr }}</span>
        </div>
        <div class="status-badge" :class="checkinStatusClass">
          {{ checkinStatusText }}
        </div>
      </div>
      <div class="check-buttons">
        <div class="check-item">
          <div class="check-time" v-if="todayAttendance?.checkInTime">{{ formatTime(todayAttendance.checkInTime) }}</div>
          <el-button type="primary" size="large" :disabled="!!todayAttendance?.checkInTime" @click="doCheckIn" class="check-btn">上班打卡</el-button>
        </div>
        <div class="check-item">
          <div class="check-time" v-if="todayAttendance?.checkOutTime">{{ formatTime(todayAttendance.checkOutTime) }}</div>
          <el-button type="warning" size="large" :disabled="!todayAttendance?.checkInTime || !!todayAttendance?.checkOutTime" @click="doCheckOut" class="check-btn">下班打卡</el-button>
        </div>
        <div class="check-item">
          <el-button type="primary" size="large" @click="openLeaveDialog" class="leave-btn">请假</el-button>
        </div>
      </div>
    </el-card>
    


    <!-- 考勤记录区域 -->
    <div class="section-title">
      <el-icon><Calendar /></el-icon>
      <span>考勤记录</span>
    </div>
    <el-card class="calendar-card" shadow="hover">
      <div class="month-selector">
        <el-button @click="prevMonth" :icon="ArrowLeft">上月</el-button>
        <span class="current-month">{{ attendanceMonthStr }}</span>
        <el-button @click="nextMonth">下月<el-icon><ArrowRight /></el-icon></el-button>
      </div>
      <div class="stats-grid">
        <div class="stat-item"><div class="stat-value">{{ workDaysCount }}</div><div class="stat-label">应到天数</div></div>
        <div class="stat-item normal"><div class="stat-value">{{ computedStats.normal }}</div><div class="stat-label">正常</div></div>
        <div class="stat-item warning"><div class="stat-value">{{ computedStats.late + computedStats.early }}</div><div class="stat-label">迟到/早退</div></div>
        <div class="stat-item"><div class="stat-value">{{ computedStats.leave }}</div><div class="stat-label">请假</div></div>
        <div class="stat-item"><div class="stat-value">{{ computedStats.absent }}</div><div class="stat-label">缺勤</div></div>
      </div>
      <div class="calendar-header">
        <div v-for="day in weekDaysName" :key="day" class="calendar-th">{{ day }}</div>
      </div>
      <div class="calendar-body">
        <div v-for="(day, index) in calendarDays" :key="index" class="calendar-cell" :class="{ 'is-empty': !day.date, 'is-today': day.isToday, 'is-workday': day.isWorkday, 'not-workday': !day.isWorkday }">
          <template v-if="day.date">
            <span class="day-number">{{ day.dayNumber }}</span>
            <div class="day-status-icon">
              <el-icon v-if="day.status === '正常'" color="#67c23a" size="16"><CircleCheck /></el-icon>
              <el-icon v-else-if="day.status === '迟到'" color="#e6a23c" size="16"><Top /></el-icon>
              <el-icon v-else-if="day.status === '早退'" color="#e6a23c" size="16"><Bottom /></el-icon>
              <el-icon v-else-if="day.status === '请假'" color="#909399" size="16"><Clock /></el-icon>
              <el-icon v-else-if="day.status === '缺勤'" color="#f56c6c" size="16"><CircleClose /></el-icon>
            </div>
          </template>
        </div>
      </div>
    </el-card>

    <!-- 请假记录区域 -->
    <div class="section-title">
      <el-icon><Document /></el-icon>
      <span>请假记录</span>
    </div>
    
    <el-card class="leave-list-card" shadow="hover">
      <div class="leave-list-header">我的请假记录</div>
      <el-table :data="leaveList" style="width: 100%">
        <el-table-column prop="leaveType" label="类型" width="100" align="center">
          <template #default="scope"><el-tag :type="getTypeTag(scope.row.leaveType)">{{ scope.row.leaveType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" align="center">
          <template #default="scope">{{ scope.row.startDate }}</template>
        </el-table-column>
        <el-table-column prop="endDate" label="结束日期" align="center">
          <template #default="scope">{{ scope.row.endDate }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" align="center" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope"><el-tag :type="getStatusTag(scope.row.status)">{{ scope.row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="approveRemark" label="审批备注" align="center" show-overflow-tooltip />
      </el-table>
    </el-card>

    <!-- 请假申请弹窗 -->
    <el-dialog v-model="leaveDialogVisible" title="请假申请" width="500px" destroy-on-close>
      <el-form :model="leaveForm" label-width="100px">
        <el-form-item label="请假类型">
          <el-select v-model="leaveForm.leaveType" placeholder="请选择请假类型" style="width: 100%">
            <el-option label="事假" value="事假" />
            <el-option label="病假" value="病假" />
            <el-option label="年假" value="年假" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="leaveForm.startDate" type="date" placeholder="选择开始日期" style="width: 100%" :disabled-date="disabledStartDate" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="leaveForm.endDate" type="date" placeholder="选择结束日期" style="width: 100%" :disabled-date="disabledEndDate" />
        </el-form-item>
        <el-form-item label="请假原因">
          <el-input v-model="leaveForm.reason" type="textarea" :rows="4" placeholder="请详细说明请假原因..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leaveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLeave">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Sunrise, Sunset, CircleCheck, Warning, CircleClose, Clock, ArrowLeft, ArrowRight, Top, Bottom, Calendar, Document } from '@element-plus/icons-vue'
import { attendanceApi, approvalApi } from '../../api'

const todayAttendance = ref(null)
const monthAttendance = ref([])
const monthStats = ref({})
const attendanceDate = ref(new Date())

// 请假相关
const leaveForm = ref({
  leaveType: '',
  startDate: '',
  endDate: '',
  reason: ''
})
const leaveList = ref([])
const leaveDialogVisible = ref(false)

const openLeaveDialog = () => {
  resetLeaveForm()
  leaveDialogVisible.value = true
}

// 打卡状态
const weekDay = computed(() => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[new Date().getDay()]
})

const currentDateStr = computed(() => {
  const now = new Date()
  return `${now.getMonth() + 1}月${now.getDate()}日`
})

const checkinStatusText = computed(() => {
  if (!todayAttendance.value) return '未打卡'
  if (todayAttendance.value.checkInTime && !todayAttendance.value.checkOutTime) return '上班已打卡'
  if (todayAttendance.value.checkInTime && todayAttendance.value.checkOutTime) return '今日已完成'
  return todayAttendance.value.status || '未打卡'
})

const checkinStatusClass = computed(() => {
  if (!todayAttendance.value?.checkInTime) return 'status-none'
  if (todayAttendance.value.status === '正常') return 'status-normal'
  if (todayAttendance.value.status === '迟到' || todayAttendance.value.status === '早退') return 'status-warning'
  return 'status-ok'
})

// 周打卡记录
const weekDays = computed(() => {
  const now = new Date()
  const day = now.getDay()
  const monday = new Date(now)
  monday.setDate(now.getDate() - (day === 0 ? 6 : day - 1))
  const days = []
  const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    const dateStr = d.toISOString().split('T')[0]
    const record = monthAttendance.value.find(r => r.date === dateStr)
    days.push({ name: dayNames[i], date: dateStr, isToday: dateStr === now.toISOString().split('T')[0], isWorkday: i < 5, status: record?.status || null })
  }
  return days
})

// 月考勤
const weekDaysName = ['一', '二', '三', '四', '五', '六', '日']
const attendanceMonthStr = computed(() => {
  const year = attendanceDate.value.getFullYear()
  const month = (attendanceDate.value.getMonth() + 1).toString().padStart(2, '0')
  return `${year}年${month}月`
})

const calendarDays = computed(() => {
  const year = attendanceDate.value.getFullYear()
  const month = attendanceDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const days = []
  const today = new Date().toISOString().split('T')[0]
  let startPadding = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1
  for (let i = 0; i < startPadding; i++) days.push({ date: null })
  for (let d = 1; d <= lastDay.getDate(); d++) {
    const dateStr = `${year}-${(month + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}`
    const record = monthAttendance.value.find(r => r.date === dateStr)
    const dayOfWeek = new Date(dateStr).getDay()
    days.push({ date: dateStr, dayNumber: d, isToday: dateStr === today, isWorkday: dayOfWeek !== 0 && dayOfWeek !== 6, status: record?.status })
  }
  return days
})

// 计算当月应到工作日天数
const workDaysCount = computed(() => {
  const year = attendanceDate.value.getFullYear()
  const month = attendanceDate.value.getMonth()
  const lastDay = new Date(year, month + 1, 0).getDate()
  let count = 0
  for (let d = 1; d <= lastDay; d++) {
    const dayOfWeek = new Date(year, month, d).getDay()
    if (dayOfWeek !== 0 && dayOfWeek !== 6) count++
  }
  return count
})

// 根据月考勤记录计算各项统计数据
const computedStats = computed(() => {
  const stats = { normal: 0, late: 0, early: 0, leave: 0, absent: 0 }
  const year = attendanceDate.value.getFullYear()
  const month = attendanceDate.value.getMonth()
  const lastDay = new Date(year, month + 1, 0).getDate()
  const today = new Date()

  for (let d = 1; d <= lastDay; d++) {
    const date = new Date(year, month, d)
    const dayOfWeek = date.getDay()
    // 只统计工作日
    if (dayOfWeek === 0 || dayOfWeek === 6) continue
    // 不统计未来日期
    if (date > today) continue

    const dateStr = `${year}-${(month + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}`
    const record = monthAttendance.value.find(r => r.date === dateStr)

    if (!record) {
      // 有记录但无考勤数据，视为缺勤
      stats.absent++
    } else {
      const status = record.status
      if (status === '正常') stats.normal++
      else if (status === '迟到') stats.late++
      else if (status === '早退') stats.early++
      else if (status === '请假') stats.leave++
      else stats.absent++
    }
  }
  return stats
})

// 考勤方法
const fetchTodayAttendance = async () => {
  try {
    const res = await attendanceApi.getToday()
    if (res.code === 200) todayAttendance.value = res.data
  } catch (e) { console.error(e) }
}

const fetchMonthAttendance = async () => {
  const year = attendanceDate.value.getFullYear()
  const month = (attendanceDate.value.getMonth() + 1).toString().padStart(2, '0')
  try {
    const res = await attendanceApi.getMy(`${year}-${month}`)
    if (res.code === 200) monthAttendance.value = res.data
  } catch (e) { console.error(e) }
}

const fetchMonthStats = async () => {
  const year = attendanceDate.value.getFullYear()
  const month = (attendanceDate.value.getMonth() + 1).toString().padStart(2, '0')
  try {
    const res = await attendanceApi.getStats(`${year}-${month}`)
    if (res.code === 200) monthStats.value = res.data
  } catch (e) { console.error(e) }
}

const doCheckIn = async () => {
  try {
    const res = await attendanceApi.checkin('checkin')
    if (res.code === 200) { ElMessage.success('打卡成功'); fetchTodayAttendance(); fetchMonthAttendance() }
    else ElMessage.error(res.message || '打卡失败')
  } catch (e) { ElMessage.error('打卡失败') }
}

const doCheckOut = async () => {
  try {
    const res = await attendanceApi.checkin('checkout')
    if (res.code === 200) { ElMessage.success('下班打卡成功'); fetchTodayAttendance(); fetchMonthAttendance() }
    else ElMessage.error(res.message || '打卡失败')
  } catch (e) { ElMessage.error('打卡失败') }
}

const formatTime = (datetime) => {
  if (!datetime) return ''
  const d = new Date(datetime)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

const prevMonth = () => {
  attendanceDate.value = new Date(attendanceDate.value.getFullYear(), attendanceDate.value.getMonth() - 1, 1)
  fetchMonthAttendance(); fetchMonthStats()
}

const nextMonth = () => {
  const now = new Date()
  const next = new Date(attendanceDate.value.getFullYear(), attendanceDate.value.getMonth() + 1, 1)
  if (next <= now) { attendanceDate.value = next; fetchMonthAttendance(); fetchMonthStats() }
  else ElMessage.warning('不能查看未来月份')
}

// 请假方法
const fetchLeaveList = async () => {
  try {
    const res = await approvalApi.getMyList({ type: 'leave' })
    if (Array.isArray(res)) {
      leaveList.value = res.map(item => ({
        id: item.id,
        leaveType: item.title?.replace('申请', '') || '请假',
        startDate: item.content?.match(/(\d{4}-\d{2}-\d{2}) 至/)?.[1] || '',
        endDate: item.content?.match(/至 (\d{4}-\d{2}-\d{2})/)?.[1] || '',
        reason: item.content?.match(/原因：(.+)$/)?.[1] || '',
        status: item.status === 'pending' ? '待审批' : (item.status === 'approved' ? '批准' : '拒绝'),
        approveRemark: item.approveRemark
      }))
    }
  } catch (e) { console.error(e) }
}

const disabledStartDate = (date) => date < new Date(new Date().setHours(0, 0, 0, 0))
const disabledEndDate = (date) => !leaveForm.value.startDate ? date < new Date() : date < leaveForm.value.startDate

const submitLeave = async () => {
  if (!leaveForm.value.leaveType) { ElMessage.warning('请选择请假类型'); return }
  if (!leaveForm.value.startDate || !leaveForm.value.endDate) { ElMessage.warning('请选择请假日期'); return }
  if (!leaveForm.value.reason) { ElMessage.warning('请填写请假原因'); return }
  const formatDate = (d) => { if (!d) return ''; if (typeof d === 'string') return d; const dd = new Date(d); return `${dd.getFullYear()}-${(dd.getMonth() + 1).toString().padStart(2, '0')}-${dd.getDate().toString().padStart(2, '0')}` }
  const data = { applicationType: 'leave', title: leaveForm.value.leaveType + '申请', content: `${formatDate(leaveForm.value.startDate)} 至 ${formatDate(leaveForm.value.endDate)}，原因：${leaveForm.value.reason}`, extraData: JSON.stringify({ startDate: formatDate(leaveForm.value.startDate), endDate: formatDate(leaveForm.value.endDate), leaveType: leaveForm.value.leaveType }) }
  try {
    const res = await approvalApi.submit(data)
    if (res && res.code === 200) { ElMessage.success('请假申请已提交'); leaveDialogVisible.value = false; resetLeaveForm(); fetchLeaveList() }
    else ElMessage.error(res?.message || '提交失败')
  } catch (e) { ElMessage.error('提交失败') }
}

const resetLeaveForm = () => { leaveForm.value = { leaveType: '', startDate: '', endDate: '', reason: '' } }

const getTypeTag = (type) => { const map = { '事假': '', '病假': 'success', '年假': 'warning' }; return map[type] || '' }
const getStatusTag = (status) => { const map = { '待审批': 'warning', '批准': 'success', '拒绝': 'danger' }; return map[status] || '' }

const loadAttendanceData = () => { fetchTodayAttendance(); fetchMonthAttendance(); fetchMonthStats(); fetchLeaveList() }

onMounted(() => {
  loadAttendanceData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 4px solid #667eea;
}

.section-title .el-icon {
  font-size: 22px;
  color: #667eea;
}

.el-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.checkin-card {
  margin-bottom: 20px;
}

.leave-list-card {
  margin-bottom: 0;
}

.work-time-info {
  display: flex;
  justify-content: center;
  gap: 50px;
  margin-bottom: 20px;
}

.time-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  color: #666;
}

.time-item .el-icon {
  font-size: 24px;
  color: #409eff;
}

.current-status {
  text-align: center;
  margin-bottom: 30px;
}

.date-display {
  margin-bottom: 15px;
}

.date-display .weekday {
  display: block;
  font-size: 24px;
  color: #666;
}

.date-display .date {
  display: block;
  font-size: 36px;
  font-weight: bold;
  color: #333;
}

.status-badge {
  display: inline-block;
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 600;
}

.status-none {
  background: #f4f4f5;
  color: #909399;
}

.status-normal {
  background: #f0f9eb;
  color: #67c23a;
}

.status-warning {
  background: #fdf6ec;
  color: #e6a23c;
}

.status-ok {
  background: #409eff;
  color: #fff;
}

.check-buttons {
  display: flex;
  justify-content: center;
  gap: 40px;
}

.check-item {
  text-align: center;
}

.check-time {
  margin-bottom: 10px;
  font-size: 18px;
  color: #666;
}

.check-btn {
  width: 140px;
  height: 50px;
  font-size: 16px;
  border-radius: 25px;
}

.leave-btn {
  width: 140px;
  height: 50px;
  font-size: 16px;
  border-radius: 25px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
}

.month-selector {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 30px;
  margin-bottom: 20px;
}

.current-month {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.stats-grid {
  display: flex;
  justify-content: space-around;
  padding: 10px 0;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-item {
  text-align: center;
  padding: 15px;
  border-radius: 12px;
  background: #f5f7fa;
  min-width: 80px;
}

.stat-item.normal {
  background: #f0f9eb;
}

.stat-item.warning {
  background: #fdf6ec;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.calendar-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #f5f7fa;
  border-radius: 8px 8px 0 0;
}

.calendar-th {
  text-align: center;
  padding: 12px;
  font-weight: 600;
  color: #666;
}

.calendar-body {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.calendar-cell {
  min-height: 70px;
  border-right: 1px solid #eee;
  border-bottom: 1px solid #eee;
  padding: 8px;
  text-align: center;
  background: white;
}

.calendar-cell:nth-child(7n) {
  border-right: none;
}

.calendar-cell.is-empty {
  background: #fafafa;
}

.calendar-cell.is-today {
  background: linear-gradient(135deg, #409eff15, #67c23a15);
}

.calendar-cell.not-workday {
  background: #fafafa;
  opacity: 0.6;
}

.day-number {
  display: block;
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
}

.day-status-icon {
  height: 20px;
}

.leave-list-header {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 15px;
}
</style>
