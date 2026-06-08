<template>
  <div class="attendance-manage">
    <h2 class="page-title">考勤管理</h2>

    <el-card class="search-card" shadow="hover">
      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="月份">
            <el-date-picker
              v-model="searchForm.month"
              type="month"
              placeholder="选择月份"
              format="YYYY-MM"
              value-format="YYYY-MM"
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="searchForm.realName" placeholder="请输入姓名" style="width: 150px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchAttendance" class="search-btn">查询</el-button>
            <el-button @click="resetSearch" class="reset-btn">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="stats-card" shadow="hover">
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">总人数</div>
        </div>
        <div class="stat-item normal">
          <div class="stat-value">{{ stats.fullAttendance || 0 }}</div>
          <div class="stat-label">全勤人数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.rate || 0 }}%</div>
          <div class="stat-label">全勤率</div>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="table-header">
          <span class="table-title">考勤汇总</span>
          <el-button type="success" @click="exportToExcel" size="small">
            导出Excel
          </el-button>
        </div>
      </template>
      <el-table :data="attendanceList" class="attendance-table" v-loading="loading" fit>
        <el-table-column prop="realName" label="姓名" align="center" min-width="80" />
        <el-table-column prop="username" label="用户名" align="center" min-width="100" />
        <el-table-column prop="month" label="月份" align="center" min-width="90" />
        <el-table-column prop="shouldAttend" label="应到" align="center" min-width="60">
          <template #default="scope">
            <span class="stat-num">{{ scope.row.shouldAttend }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="normal" label="出勤" align="center" min-width="60">
          <template #default="scope">
            <el-tag type="success" size="small">{{ scope.row.normal }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="late" label="迟到" align="center" min-width="60">
          <template #default="scope">
            <el-tag type="warning" size="small" v-if="scope.row.late > 0">{{ scope.row.late }}</el-tag>
            <span v-else class="stat-num">0</span>
          </template>
        </el-table-column>
        <el-table-column prop="early" label="早退" align="center" min-width="60">
          <template #default="scope">
            <el-tag type="warning" size="small" v-if="scope.row.early > 0">{{ scope.row.early }}</el-tag>
            <span v-else class="stat-num">0</span>
          </template>
        </el-table-column>
        <el-table-column prop="leave" label="请假" align="center" min-width="60">
          <template #default="scope">
            <el-tag type="info" size="small" v-if="scope.row.leave > 0">{{ scope.row.leave }}</el-tag>
            <span v-else class="stat-num">0</span>
          </template>
        </el-table-column>
        <el-table-column prop="absent" label="缺勤" align="center" min-width="60">
          <template #default="scope">
            <el-tag type="danger" size="small" v-if="scope.row.absent > 0">{{ scope.row.absent }}</el-tag>
            <span v-else class="stat-num">0</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { attendanceApi } from '../../api/attendance'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

const loading = ref(false)
const attendanceList = ref([])

const searchForm = ref({
  month: '',
  realName: ''
})

const stats = ref({
  total: 0,
  fullAttendance: 0,
  rate: 0
})

// 初始化月份为当前月份
const now = new Date()
searchForm.value.month = `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, '0')}`

onMounted(() => {
  fetchAttendance()
})

const fetchAttendance = async () => {
  loading.value = true
  try {
    const res = await attendanceApi.getAll(searchForm.value.month)
    if (res.code === 200) {
      let userData = res.data || []

      // 按姓名筛选
      if (searchForm.value.realName) {
        userData = userData.filter(item =>
          (item.realName && item.realName.includes(searchForm.value.realName)) ||
          (item.username && item.username.includes(searchForm.value.realName))
        )
      }

      // 直接使用汇总数据
      attendanceList.value = userData

      // 计算统计汇总
      const total = userData.length
      // 全勤：late=0, early=0, leave=0, absent=0
      const fullAttendance = userData.filter(item =>
        (item.late || 0) === 0 &&
        (item.early || 0) === 0 &&
        (item.leave || 0) === 0 &&
        (item.absent || 0) === 0
      ).length
      const rate = total > 0 ? Math.round((fullAttendance / total) * 100) : 0

      stats.value = {
        total,
        fullAttendance,
        rate
      }
    }
  } catch (error) {
    console.error('获取考勤记录失败:', error)
    ElMessage.error('获取考勤记录失败')
  } finally {
    loading.value = false
  }
}

// 导出 Excel
const exportToExcel = () => {
  const data = attendanceList.value

  if (data.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  const exportData = data.map(item => ({
    '姓名': item.realName || '',
    '用户名': item.username || '',
    '月份': item.month || '',
    '应到': item.shouldAttend || 0,
    '出勤': item.normal || 0,
    '迟到': item.late || 0,
    '早退': item.early || 0,
    '请假': item.leave || 0,
    '缺勤': item.absent || 0
  }))

  const worksheet = XLSX.utils.json_to_sheet(exportData)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '考勤汇总')

  worksheet['!cols'] = [
    { wch: 10 }, { wch: 12 }, { wch: 10 },
    { wch: 8 }, { wch: 8 }, { wch: 8 },
    { wch: 8 }, { wch: 8 }, { wch: 8 }
  ]

  const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
  const blob = new Blob([excelBuffer], { type: 'application/octet-stream' })
  const monthStr = searchForm.value.month || ''
  saveAs(blob, `考勤汇总_${monthStr}.xlsx`)

  ElMessage.success('导出成功')
}

const resetSearch = () => {
  const now = new Date()
  searchForm.value = {
    month: `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, '0')}`,
    realName: ''
  }
  fetchAttendance()
}

// 监听月份变化
watch(() => searchForm.value.month, () => {
  fetchAttendance()
})
</script>

<style scoped>
.attendance-manage {
  background-color: transparent;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 30px;
}

.attendance-manage::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 30%, rgba(64, 158, 255, 0.08) 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(103, 194, 58, 0.08) 0%, transparent 40%);
  z-index: 0;
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
  background: linear-gradient(90deg, #409eff, #67c23a);
  margin: 20px auto 0;
  border-radius: 2px;
  animation: expand 1.5s ease-out 0.5s both;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

@keyframes expand {
  from { width: 0; }
  to { width: 100px; }
}

.search-card, .stats-card, .table-card {
  margin-bottom: 20px;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  background: white;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.1);
  animation: fadeIn 0.8s ease-out both;
}

.search-card { animation-delay: 0.1s; }
.stats-card { animation-delay: 0.2s; }
.table-card { animation-delay: 0.3s; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.search-form {
  padding: 20px;
}

.search-btn {
  background: linear-gradient(90deg, #409eff, #67c23a);
  border: none;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.reset-btn {
  border-radius: 6px;
  transition: all 0.3s ease;
}

.reset-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stats-grid {
  display: flex;
  justify-content: space-around;
  padding: 20px;
  gap: 20px;
  flex-wrap: wrap;
}

.stat-item {
  text-align: center;
  padding: 20px 30px;
  border-radius: 12px;
  background: #f5f7fa;
  min-width: 120px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.stat-item.normal {
  background: linear-gradient(135deg, #f0f9eb, #e1f3d8);
}

.stat-item.warning {
  background: linear-gradient(135deg, #fdf6ec, #faecd8);
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-title {
  font-weight: 600;
  font-size: 16px;
}

.attendance-table {
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}

.attendance-table th {
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  font-weight: 600;
  color: #333;
}

.attendance-table tr:hover {
  background-color: rgba(64, 158, 255, 0.05);
  transition: background-color 0.3s ease;
}

.stat-num {
  color: #909399;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}

@media (max-width: 768px) {
  .attendance-manage {
    padding: 20px;
  }

  .page-title {
    font-size: 1.6rem;
  }

  .stats-grid {
    flex-wrap: wrap;
    gap: 10px;
  }

  .stat-item {
    min-width: 100px;
    padding: 15px;
  }

  .stat-value {
    font-size: 24px;
  }
}
</style>
