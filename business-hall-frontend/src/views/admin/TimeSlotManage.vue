<template>
  <div class="time-slot-manage">
    <h2 class="page-title">时段管理</h2>
    
    <div class="page-header">
      <el-button type="primary" @click="generateTimeSlots" class="generate-btn">生成时段</el-button>
    </div>
    
    <el-card class="search-card" shadow="hover">
      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="日期">
            <el-date-picker
              v-model="searchForm.date"
              type="date"
              placeholder="选择日期"
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.isActive" placeholder="选择状态" style="width: 100px">
              <el-option label="活跃" value="1" />
              <el-option label="不活跃" value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchTimeSlots" class="search-btn">查询</el-button>
            <el-button @click="resetSearch" class="reset-btn">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    
    <el-card class="table-card" shadow="hover">
      <el-table :data="timeSlots" style="width: 100%" class="time-slot-table">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="date" label="日期" align="center" />
        <el-table-column prop="startTime" label="开始时间" align="center" />
        <el-table-column prop="endTime" label="结束时间" align="center" />
        <el-table-column prop="totalQuota" label="总号源" width="100" align="center" />
        <el-table-column prop="remainingQuota" label="剩余号源" width="100" align="center" />
        <el-table-column prop="businessTypeId" label="业务类型ID" width="120" align="center" />
        <el-table-column prop="isActive" label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.isActive === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.isActive === 1 ? '活跃' : '不活跃' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="center">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="updateStatus(scope.row.id, scope.row.isActive === 1 ? 0 : 1)"
              style="margin-right: 8px"
            >
              {{ scope.row.isActive === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="deleteTimeSlot(scope.row.id)"
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { timeslotApi } from '../../api'
import { ElMessage } from 'element-plus'
import { formatDate } from '../../utils/dateUtil'

const timeSlots = ref([])
const searchForm = ref({
  date: '',
  isActive: ''
})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(() => {
  fetchTimeSlots()
})

const fetchTimeSlots = async () => {
  try {
    const res = await timeslotApi.list()
    if (res.code === 200) {
      let data = res.data
      
      // 筛选
      if (searchForm.value.date) {
        const searchDate = formatDate(searchForm.value.date)
        data = data.filter(slot => slot.date === searchDate)
      }
      if (searchForm.value.isActive !== '') {
        data = data.filter(slot => slot.isActive === parseInt(searchForm.value.isActive))
      }
      
      // 分页
      total.value = data.length
      const startIndex = (currentPage.value - 1) * pageSize.value
      const endIndex = startIndex + pageSize.value
      timeSlots.value = data.slice(startIndex, endIndex)
    }
  } catch (error) {
    console.error('获取时段列表失败:', error)
    ElMessage.error('获取时段列表失败')
  }
}

const resetSearch = () => {
  searchForm.value = {
    date: '',
    isActive: ''
  }
  currentPage.value = 1
  fetchTimeSlots()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchTimeSlots()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  fetchTimeSlots()
}

const generateTimeSlots = async () => {
  try {
    const res = await timeslotApi.generate()
    if (res.code === 200) {
      ElMessage.success(res.message || '时段生成成功')
      fetchTimeSlots()
    } else {
      ElMessage.error(res.message || '时段生成失败')
    }
  } catch (error) {
    ElMessage.error('时段生成失败')
  }
}

const updateStatus = async (timeSlotId, isActive) => {
  try {
    const res = await timeslotApi.updateStatus(timeSlotId, isActive)
    if (res.code === 200) {
      ElMessage.success(res.message || '状态更新成功')
      fetchTimeSlots()
    } else {
      ElMessage.error(res.message || '状态更新失败')
    }
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

const deleteTimeSlot = async (timeSlotId) => {
  try {
    const res = await timeslotApi.delete(timeSlotId)
    if (res.code === 200) {
      ElMessage.success(res.message || '时段删除成功')
      fetchTimeSlots()
    } else {
      ElMessage.error(res.message || '时段删除失败')
    }
  } catch (error) {
    ElMessage.error('时段删除失败')
  }
}
</script>

<style scoped>
.time-slot-manage {
  background-color: transparent;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 30px;
}

.time-slot-manage::before {
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

.page-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.generate-btn {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.generate-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
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

.time-slot-table {
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}

.time-slot-table th {
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  font-weight: 600;
  color: #333;
}

.time-slot-table tr:hover {
  background-color: rgba(102, 126, 234, 0.05);
  transition: background-color 0.3s ease;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
  padding: 0 20px 20px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .time-slot-manage { padding: 20px; }
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