<template>
  <div class="appointment-manage">
    <h2 class="page-title">预约管理</h2>
    
    <el-card class="search-card" shadow="hover">
      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="客户姓名">
            <el-input v-model="searchForm.customerName" placeholder="请输入客户姓名" style="width: 150px" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="searchForm.customerPhone" placeholder="请输入手机号" style="width: 150px" />
          </el-form-item>
          <el-form-item label="预约日期">
            <el-date-picker
              v-model="searchForm.date"
              type="date"
              placeholder="选择日期"
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="选择状态" style="width: 120px">
              <el-option label="待办理" value="待办理" />
              <el-option label="办理中" value="办理中" />
              <el-option label="已完成" value="已完成" />
              <el-option label="已取消" value="已取消" />
              <el-option label="爽约" value="爽约" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchAppointments" class="search-btn">查询</el-button>
            <el-button @click="resetSearch" class="reset-btn">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="table-header">
          <span class="table-title">预约列表</span>
          <div class="table-actions">
            <el-button type="success" :icon="Download" @click="exportToExcel" size="small">
              导出Excel
            </el-button>
            <el-button type="warning" :icon="Folder" @click="showArchiveDialog" size="small">
              归档
            </el-button>
            <el-button type="danger" :icon="Delete" @click="showBatchDeleteDialog" size="small">
              批量清除
            </el-button>
            <el-button type="info" :icon="FolderOpened" @click="showArchiveListDialog" size="small">
              归档记录
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="appointments" style="width: 100%" class="appointment-table" v-loading="loading">
        <el-table-column prop="id" label="预约编号" width="100" align="center" />
        <el-table-column prop="customerName" label="客户姓名" align="center" />
        <el-table-column prop="customerPhone" label="手机号" align="center" />
        <el-table-column prop="businessType" label="业务类型" align="center" />
        <el-table-column prop="appointmentDate" label="预约日期" align="center" />
        <el-table-column prop="appointmentTimeRange" label="预约时段" align="center" />
        <el-table-column label="核验码" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.verificationCode" style="color: #e65100; font-weight: 700; font-family: 'Courier New', monospace; letter-spacing: 2px;">{{ scope.row.verificationCode }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="clerkName" label="办理营业员" width="110" align="center" />
        <el-table-column prop="status" label="状态" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理结果" width="150" align="center">
          <template #default="scope">
            <span v-if="scope.row.result">{{ scope.row.result }}</span>
            <span v-else style="color: #c0c4cc;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" align="center" />
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

    <!-- 批量清除对话框 -->
    <el-dialog
      v-model="batchDeleteDialogVisible"
      title="批量清除预约"
      width="500px"
      destroy-on-close
    >
      <el-form :model="batchDeleteForm" label-width="100px">
        <el-form-item label="状态筛选">
          <el-select v-model="batchDeleteForm.status" placeholder="全部状态" clearable style="width: 100%">
            <el-option label="全部状态" value="" />
            <el-option label="待办理" value="待办理" />
            <el-option label="办理中" value="办理中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已取消" value="已取消" />
            <el-option label="爽约" value="爽约" />
          </el-select>
          <div class="form-tip">不选择则清除所有状态的预约</div>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="batchDeleteForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item>
          <div class="preview-info">
            <el-alert
              v-if="previewCount !== null"
              :title="`预计清除 ${previewCount} 条预约记录`"
              :type="previewCount > 0 ? 'warning' : 'info'"
              :closable="false"
              show-icon
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDeleteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="previewBatchDelete" :loading="previewLoading">
          预览数量
        </el-button>
        <el-button
          type="danger"
          @click="confirmBatchDelete"
          :loading="deleteLoading"
          :disabled="previewCount === 0 || previewCount === null"
        >
          确认清除
        </el-button>
      </template>
    </el-dialog>

    <!-- 归档对话框 -->
    <el-dialog
      v-model="archiveDialogVisible"
      title="预约归档"
      width="500px"
      destroy-on-close
    >
      <el-form :model="archiveForm" label-width="100px">
        <el-form-item label="截止日期" required>
          <el-date-picker
            v-model="archiveForm.endDate"
            type="date"
            placeholder="选择截止日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
          <div class="form-tip">将归档此日期及之前的预约记录</div>
        </el-form-item>
        <el-form-item label="状态筛选">
          <el-checkbox-group v-model="archiveForm.statuses">
            <el-checkbox label="已完成">已完成</el-checkbox>
            <el-checkbox label="已取消">已取消</el-checkbox>
            <el-checkbox label="爽约">爽约</el-checkbox>
            <el-checkbox label="待办理">待办理</el-checkbox>
          </el-checkbox-group>
          <div class="form-tip">不选择则归档所有状态的预约</div>
        </el-form-item>
        <el-form-item>
          <div class="preview-info">
            <el-alert
              v-if="archivePreviewCount !== null"
              :title="`预计归档 ${archivePreviewCount} 条预约记录`"
              :type="archivePreviewCount > 0 ? 'warning' : 'info'"
              :closable="false"
              show-icon
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="archiveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="previewArchive" :loading="archivePreviewLoading">
          预览数量
        </el-button>
        <el-button
          type="warning"
          @click="confirmArchive"
          :loading="archiveLoading"
          :disabled="archivePreviewCount === 0 || archivePreviewCount === null"
        >
          确认归档
        </el-button>
      </template>
    </el-dialog>

    <!-- 归档记录对话框 -->
    <el-dialog
      v-model="archiveListDialogVisible"
      title="归档记录"
      width="900px"
      destroy-on-close
    >
      <div class="archive-search">
        <el-form :inline="true" :model="archiveSearchForm">
          <el-form-item label="客户姓名">
            <el-input v-model="archiveSearchForm.customerName" placeholder="请输入" style="width: 120px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="archiveSearchForm.status" placeholder="全部" clearable style="width: 100px">
              <el-option label="待办理" value="待办理" />
              <el-option label="办理中" value="办理中" />
              <el-option label="已完成" value="已完成" />
              <el-option label="已取消" value="已取消" />
              <el-option label="爽约" value="爽约" />
            </el-select>
          </el-form-item>
          <el-form-item label="预约日期">
            <el-date-picker
              v-model="archiveSearchForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              style="width: 220px"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchArchiveList">查询</el-button>
            <el-button @click="resetArchiveSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-table
        :data="archiveList"
        style="width: 100%"
        v-loading="archiveListLoading"
        height="400"
        @selection-change="handleArchiveSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="appointmentNo" label="预约编号" width="120" align="center" />
        <el-table-column prop="customerName" label="客户姓名" align="center" />
        <el-table-column prop="customerPhone" label="手机号" align="center" />
        <el-table-column prop="businessType" label="业务类型" align="center" />
        <el-table-column prop="appointmentDate" label="预约日期" align="center" />
        <el-table-column prop="status" label="状态" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="archivedAt" label="归档时间" align="center" width="160" />
      </el-table>
      <template #footer>
        <el-button @click="archiveListDialogVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="selectedArchives.length === 0" @click="restoreSelectedArchives">
          恢复选中记录
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { appointmentApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '../../utils/dateUtil'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'
import { Download, Delete, Folder, FolderOpened } from '@element-plus/icons-vue'

const appointments = ref([])
const searchForm = ref({
  customerName: '',
  customerPhone: '',
  date: '',
  status: ''
})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

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
      return 'info'
  }
}

onMounted(() => {
  fetchAppointments()
})

const resetSearch = () => {
  searchForm.value = {
    customerName: '',
    customerPhone: '',
    date: '',
    status: ''
  }
  currentPage.value = 1
  fetchAppointments()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchAppointments()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  fetchAppointments()
}

// 导出 Excel
const exportToExcel = () => {
  // 获取当前筛选后的数据
  let data = [...allAppointments.value]

  if (searchForm.value.customerName) {
    data = data.filter(app => app.customerName.includes(searchForm.value.customerName))
  }
  if (searchForm.value.customerPhone) {
    data = data.filter(app => app.customerPhone.includes(searchForm.value.customerPhone))
  }
  if (searchForm.value.date) {
    const searchDate = formatDate(searchForm.value.date)
    data = data.filter(app => app.date === searchDate)
  }
  if (searchForm.value.status) {
    data = data.filter(app => app.status === searchForm.value.status)
  }

  if (data.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  // 准备导出数据
  const exportData = data.map(item => ({
    '预约编号': item.id,
    '客户姓名': item.customerName,
    '手机号': item.customerPhone,
    '业务类型': item.businessType,
    '预约日期': item.appointmentDate,
    '预约时段': item.appointmentTimeRange,
    '状态': item.status,
    '创建时间': item.createTime
  }))

  // 创建工作簿
  const worksheet = XLSX.utils.json_to_sheet(exportData)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '预约记录')

  // 设置列宽
  worksheet['!cols'] = [
    { wch: 12 },  // 预约编号
    { wch: 12 },  // 客户姓名
    { wch: 15 },  // 手机号
    { wch: 15 },  // 业务类型
    { wch: 12 },  // 预约日期
    { wch: 15 },  // 预约时段
    { wch: 10 },  // 状态
    { wch: 20 }   // 创建时间
  ]

  // 生成文件并下载
  const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
  const blob = new Blob([excelBuffer], { type: 'application/octet-stream' })
  const fileName = `预约记录_${formatDate(new Date())}.xlsx`
  saveAs(blob, fileName)

  ElMessage.success('导出成功')
}

// 存储所有预约数据（用于导出）
const allAppointments = ref([])

// 修改 fetchAppointments 保存所有数据
const fetchAllAppointments = async () => {
  try {
    let data = await appointmentApi.list()
    allAppointments.value = data || []
    applyFilters()
  } catch (error) {
    console.error('获取预约列表失败:', error)
    ElMessage.error('获取预约列表失败')
  }
}

// 应用筛选
const applyFilters = () => {
  let data = [...allAppointments.value]

  if (searchForm.value.customerName) {
    data = data.filter(app => app.customerName.includes(searchForm.value.customerName))
  }
  if (searchForm.value.customerPhone) {
    data = data.filter(app => app.customerPhone.includes(searchForm.value.customerPhone))
  }
  if (searchForm.value.date) {
    const searchDate = formatDate(searchForm.value.date)
    data = data.filter(app => app.date === searchDate)
  }
  if (searchForm.value.status) {
    data = data.filter(app => app.status === searchForm.value.status)
  }

  total.value = data.length
  const startIndex = (currentPage.value - 1) * pageSize.value
  const endIndex = startIndex + pageSize.value
  appointments.value = data.slice(startIndex, endIndex)
}

// 重写 fetchAppointments
const fetchAppointments = async () => {
  loading.value = true
  try {
    await fetchAllAppointments()
  } finally {
    loading.value = false
  }
}

// 添加 loading 状态
const loading = ref(false)

// 批量清除相关
const batchDeleteDialogVisible = ref(false)
const batchDeleteForm = ref({
  status: '',
  dateRange: []
})
const previewCount = ref(null)
const previewLoading = ref(false)
const deleteLoading = ref(false)

// 显示批量清除对话框
const showBatchDeleteDialog = () => {
  batchDeleteForm.value = {
    status: '',
    dateRange: []
  }
  previewCount.value = null
  batchDeleteDialogVisible.value = true
}

// 预览批量清除数量
const previewBatchDelete = () => {
  const count = calculateDeleteCount()
  previewCount.value = count
  if (count === 0) {
    ElMessage.info('没有符合条件的预约记录')
  }
}

// 计算可删除数量
const calculateDeleteCount = () => {
  let data = [...allAppointments.value]

  // 状态筛选
  if (batchDeleteForm.value.status) {
    data = data.filter(app => app.status === batchDeleteForm.value.status)
  }

  // 日期范围筛选
  if (batchDeleteForm.value.dateRange && batchDeleteForm.value.dateRange.length === 2) {
    const startDate = new Date(batchDeleteForm.value.dateRange[0]).getTime()
    const endDate = new Date(batchDeleteForm.value.dateRange[1]).getTime()
    data = data.filter(app => {
      if (!app.appointmentDate) return false
      const appDate = new Date(app.appointmentDate).getTime()
      return appDate >= startDate && appDate <= endDate
    })
  }

  return data.length
}

// 确认批量清除
const confirmBatchDelete = () => {
  if (previewCount.value === 0 || previewCount.value === null) {
    ElMessage.warning('没有可清除的记录')
    return
  }

  ElMessageBox.confirm(
    `确定要清除 ${previewCount.value} 条预约记录吗？此操作不可恢复！`,
    '确认清除',
    {
      confirmButtonText: '确定清除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    executeBatchDelete()
  }).catch(() => {
    // 取消
  })
}

// 执行批量清除
const executeBatchDelete = async () => {
  deleteLoading.value = true
  try {
    // 获取要删除的记录ID列表
    let data = [...allAppointments.value]

    if (batchDeleteForm.value.status) {
      data = data.filter(app => app.status === batchDeleteForm.value.status)
    }

    if (batchDeleteForm.value.dateRange && batchDeleteForm.value.dateRange.length === 2) {
      const startDate = new Date(batchDeleteForm.value.dateRange[0]).getTime()
      const endDate = new Date(batchDeleteForm.value.dateRange[1]).getTime()
      data = data.filter(app => {
        if (!app.appointmentDate) return false
        const appDate = new Date(app.appointmentDate).getTime()
        return appDate >= startDate && appDate <= endDate
      })
    }

    const ids = data.map(app => app.id)

    // 调用后端批量删除接口
    await appointmentApi.batchDelete(ids)

    ElMessage.success(`成功清除 ${ids.length} 条预约记录`)
    batchDeleteDialogVisible.value = false
    fetchAppointments()
  } catch (error) {
    console.error('批量清除失败:', error)
    ElMessage.error('批量清除失败')
  } finally {
    deleteLoading.value = false
  }
}

// ==================== 归档功能 ====================
const archiveDialogVisible = ref(false)
const archiveForm = ref({
  endDate: '',
  statuses: []
})
const archivePreviewCount = ref(null)
const archivePreviewLoading = ref(false)
const archiveLoading = ref(false)

// 显示归档对话框
const showArchiveDialog = () => {
  // 默认设置为3个月前的日期
  const threeMonthsAgo = new Date()
  threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3)
  archiveForm.value = {
    endDate: formatDate(threeMonthsAgo),
    statuses: ['已完成', '已取消', '爽约']
  }
  archivePreviewCount.value = null
  archiveDialogVisible.value = true
}

// 预览可归档数量
const previewArchive = async () => {
  if (!archiveForm.value.endDate) {
    ElMessage.warning('请选择截止日期')
    return
  }

  archivePreviewLoading.value = true
  try {
    const res = await appointmentApi.previewArchive({
      endDate: archiveForm.value.endDate,
      statuses: archiveForm.value.statuses.length > 0 ? archiveForm.value.statuses : null
    })
    archivePreviewCount.value = res.count
    if (res.count === 0) {
      ElMessage.info('没有符合条件的预约记录')
    }
  } catch (error) {
    console.error('预览归档数量失败:', error)
    ElMessage.error('预览失败')
  } finally {
    archivePreviewLoading.value = false
  }
}

// 确认归档
const confirmArchive = () => {
  if (archivePreviewCount.value === 0 || archivePreviewCount.value === null) {
    ElMessage.warning('没有可归档的记录')
    return
  }

  ElMessageBox.confirm(
    `确定要归档 ${archivePreviewCount.value} 条预约记录吗？归档后将从主表移除，可在归档记录中查看。`,
    '确认归档',
    {
      confirmButtonText: '确定归档',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    executeArchive()
  }).catch(() => {
    // 取消
  })
}

// 执行归档
const executeArchive = async () => {
  archiveLoading.value = true
  try {
    const res = await appointmentApi.archiveAppointments({
      endDate: archiveForm.value.endDate,
      statuses: archiveForm.value.statuses.length > 0 ? archiveForm.value.statuses : null
    })
    ElMessage.success(res.message || `成功归档 ${archivePreviewCount.value} 条预约记录`)
    archiveDialogVisible.value = false
    fetchAppointments()
  } catch (error) {
    console.error('归档失败:', error)
    ElMessage.error('归档失败')
  } finally {
    archiveLoading.value = false
  }
}

// ==================== 归档记录功能 ====================
const archiveListDialogVisible = ref(false)
const archiveList = ref([])
const archiveListLoading = ref(false)
const archiveSearchForm = ref({
  customerName: '',
  status: '',
  dateRange: []
})
const selectedArchives = ref([])

// 显示归档记录对话框
const showArchiveListDialog = () => {
  archiveSearchForm.value = {
    customerName: '',
    status: '',
    dateRange: []
  }
  selectedArchives.value = []
  archiveListDialogVisible.value = true
  fetchArchiveList()
}

// 获取归档记录列表
const fetchArchiveList = async () => {
  archiveListLoading.value = true
  try {
    const params = {
      customerName: archiveSearchForm.value.customerName,
      status: archiveSearchForm.value.status
    }
    if (archiveSearchForm.value.dateRange && archiveSearchForm.value.dateRange.length === 2) {
      params.startDate = archiveSearchForm.value.dateRange[0]
      params.endDate = archiveSearchForm.value.dateRange[1]
    }
    const res = await appointmentApi.getArchiveList(params)
    archiveList.value = res || []
  } catch (error) {
    console.error('获取归档记录失败:', error)
    ElMessage.error('获取归档记录失败')
  } finally {
    archiveListLoading.value = false
  }
}

// 重置归档搜索
const resetArchiveSearch = () => {
  archiveSearchForm.value = {
    customerName: '',
    status: '',
    dateRange: []
  }
  fetchArchiveList()
}

// 处理归档记录选择变化
const handleArchiveSelectionChange = (selection) => {
  selectedArchives.value = selection.map(item => item.id)
}

// 恢复选中的归档记录
const restoreSelectedArchives = async () => {
  if (selectedArchives.value.length === 0) {
    ElMessage.warning('请选择要恢复的记录')
    return
  }

  ElMessageBox.confirm(
    `确定要恢复 ${selectedArchives.value.length} 条归档记录到主表吗？`,
    '确认恢复',
    {
      confirmButtonText: '确定恢复',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await appointmentApi.restoreArchive(selectedArchives.value)
      ElMessage.success(res.message || '恢复成功')
      fetchArchiveList()
      fetchAppointments()
    } catch (error) {
      console.error('恢复失败:', error)
      ElMessage.error('恢复失败')
    }
  }).catch(() => {
    // 取消
  })
}
</script>

<style scoped>
.appointment-manage {
  background-color: transparent;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 30px;
}

.appointment-manage::before {
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

.appointment-table {
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}

.appointment-table th {
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  font-weight: 600;
  color: #333;
}

.appointment-table tr:hover {
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

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-title {
  font-weight: 600;
  font-size: 16px;
}

.table-actions {
  display: flex;
  gap: 10px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.preview-info {
  width: 100%;
}

.archive-search {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .appointment-manage { padding: 20px; }
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