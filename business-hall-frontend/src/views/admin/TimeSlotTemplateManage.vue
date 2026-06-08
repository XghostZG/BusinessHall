<template>
  <div class="time-slot-template-manage">
    <h2 class="page-title">时段模板管理</h2>
    
    <div class="page-header">
      <el-button type="primary" @click="openCreateDialog" class="create-btn">创建时段模板</el-button>
    </div>
    
    <el-card class="search-card" shadow="hover">
      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="星期">
            <el-select v-model="searchForm.weekday" placeholder="选择星期" style="width: 120px">
              <el-option label="周一" value="1"></el-option>
              <el-option label="周二" value="2"></el-option>
              <el-option label="周三" value="3"></el-option>
              <el-option label="周四" value="4"></el-option>
              <el-option label="周五" value="5"></el-option>
              <el-option label="周六" value="6"></el-option>
              <el-option label="周日" value="7"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="业务类型">
            <el-select v-model="searchForm.businessTypeId" placeholder="选择业务类型" style="width: 180px">
              <el-option v-for="type in businessTypes" :key="type.id" :label="type.name" :value="type.id"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchTemplates" class="search-btn">查询</el-button>
            <el-button @click="resetSearch" class="reset-btn">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    
    <el-card class="table-card" shadow="hover">
      <el-table :data="templates" style="width: 100%" :default-sort="{prop: 'id', order: 'asc'}" class="template-table">
        <el-table-column prop="id" label="模板ID" width="80" fixed align="center"></el-table-column>
        <el-table-column prop="weekday" label="星期" width="80" align="center">
          <template #default="scope">
            {{ getWeekdayText(scope.row.weekday) }}
          </template>
        </el-table-column>
        <el-table-column prop="businessTypeName" label="业务类型" min-width="150" align="center"></el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="120" align="center"></el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="120" align="center"></el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="100" align="center"></el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openEditDialog(scope.row)" class="edit-btn">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteTemplate(scope.row.id)" class="delete-btn">删除</el-button>
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
    
    <!-- 创建/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      class="template-dialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="dialog-form">
        <el-form-item label="星期" prop="weekday">
          <el-select v-model="form.weekday" placeholder="选择星期" style="width: 100%">
            <el-option label="周一" value="1"></el-option>
            <el-option label="周二" value="2"></el-option>
            <el-option label="周三" value="3"></el-option>
            <el-option label="周四" value="4"></el-option>
            <el-option label="周五" value="5"></el-option>
            <el-option label="周六" value="6"></el-option>
            <el-option label="周日" value="7"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型" prop="businessTypeId">
          <el-select v-model="form.businessTypeId" placeholder="选择业务类型" style="width: 100%">
            <el-option v-for="type in businessTypes" :key="type.id" :label="type.name" :value="type.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker
            v-model="form.startTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择开始时间"
            style="width: 100%"
          ></el-time-picker>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker
            v-model="form.endTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择结束时间"
            style="width: 100%"
          ></el-time-picker>
        </el-form-item>
        <el-form-item label="时长(分钟)" prop="duration">
          <el-input-number v-model="form.duration" :min="1" :max="120" style="width: 100%"></el-input-number>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" class="cancel-btn">取消</el-button>
          <el-button type="primary" @click="saveTemplate" class="save-btn">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { businessApi, timeslotApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

// 响应式数据
const templates = ref([])
const businessTypes = ref([])
const searchForm = ref({
  weekday: '',
  businessTypeId: ''
})
const form = ref({
  id: '',
  weekday: '',
  businessTypeId: '',
  startTime: '',
  endTime: '',
  duration: ''
})
const rules = {
  weekday: [{ required: true, message: '请选择星期', trigger: 'change' }],
  businessTypeId: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  duration: [{ required: true, message: '请输入时长', trigger: 'blur' }]
}
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 生命周期
onMounted(() => {
  loadBusinessTypes()
  loadTemplates()
})

// 加载业务类型
const loadBusinessTypes = async () => {
  try {
    const res = await businessApi.list()
    if (res.code === 200) {
      businessTypes.value = res.data
    } else {
      businessTypes.value = []
    }
  } catch (error) {
    ElMessage.error('加载业务类型失败，部分功能可能不可用')
    console.error(error)
    businessTypes.value = []
  }
}

// 加载时段模板
const loadTemplates = async () => {
  try {
    const res = await timeslotApi.getTemplate()
    if (res.code === 200) {
      const data = res.data
      total.value = data.length
      // 分页处理
      const startIndex = (currentPage.value - 1) * pageSize.value
      const endIndex = startIndex + pageSize.value
      templates.value = data.slice(startIndex, endIndex)
    }
  } catch (error) {
    ElMessage.error('加载时段模板失败')
    console.error(error)
  }
}

// 按条件查询
const searchTemplates = async () => {
  if (!searchForm.value.weekday) {
    ElMessage.warning('请选择星期')
    return
  }
  
  if (!searchForm.value.businessTypeId) {
    ElMessage.warning('请选择业务类型')
    return
  }
  
  // 转换数据类型
  const weekday = parseInt(searchForm.value.weekday)
  const businessTypeId = parseInt(searchForm.value.businessTypeId)
  
  try {
    const res = await timeslotApi.getTemplatesByConditions(weekday, businessTypeId)
    if (res.code === 200) {
      const data = res.data
      total.value = data.length
      // 分页处理
      const startIndex = (currentPage.value - 1) * pageSize.value
      const endIndex = startIndex + pageSize.value
      templates.value = data.slice(startIndex, endIndex)
    }
  } catch (error) {
    ElMessage.error('查询时段模板失败')
    console.error(error)
  }
}

// 重置查询
const resetSearch = () => {
  searchForm.value = {
    weekday: '',
    businessTypeId: ''
  }
  currentPage.value = 1
  loadTemplates()
}

// 打开创建对话框
const openCreateDialog = () => {
  form.value = {
    id: '',
    weekday: '',
    businessTypeId: '',
    startTime: '',
    endTime: '',
    duration: ''
  }
  dialogTitle.value = '创建时段模板'
  dialogVisible.value = true
}

// 打开编辑对话框
const openEditDialog = (template) => {
  form.value = { ...template }
  dialogTitle.value = '编辑时段模板'
  dialogVisible.value = true
}

// 保存模板
const saveTemplate = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      // 转换数据类型
      const templateData = {
        ...form.value,
        weekday: parseInt(form.value.weekday),
        businessTypeId: parseInt(form.value.businessTypeId),
        duration: parseInt(form.value.duration)
      }
      
      try {
        if (form.value.id) {
          // 更新模板
          const res = await timeslotApi.updateTemplate(templateData)
          if (res.code === 200) {
            ElMessage.success('时段模板更新成功')
            dialogVisible.value = false
            // 重新加载当前页数据
            loadTemplates()
          } else {
            ElMessage.error(res.message || '时段模板更新失败')
          }
        } else {
          // 创建模板
          const res = await timeslotApi.createTemplate(templateData)
          if (res.code === 200) {
            ElMessage.success('时段模板创建成功')
            dialogVisible.value = false
            // 重置到第一页，显示新创建的数据
            currentPage.value = 1
            loadTemplates()
          } else {
            ElMessage.error(res.message || '时段模板创建失败')
          }
        }
      } catch (error) {
        ElMessage.error('时段模板保存失败')
        console.error(error)
      }
    }
  })
}

// 删除模板
const deleteTemplate = (id) => {
  ElMessageBox.confirm('确定要删除这个时段模板吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  .then(async () => {
    try {
      const res = await timeslotApi.deleteTemplate(id)
      if (res.code === 200) {
        ElMessage.success('时段模板删除成功')
        // 检查当前页是否还有数据，如果没有则跳转到上一页
        if (templates.value.length === 1 && currentPage.value > 1) {
          currentPage.value--
        }
        loadTemplates()
      } else {
        ElMessage.error(res.message || '时段模板删除失败')
      }
    } catch (error) {
      ElMessage.error('时段模板删除失败')
      console.error(error)
    }
  })
  .catch(() => {
    // 取消删除
  })
}

// 获取星期文本
const getWeekdayText = (weekday) => {
  const weekdays = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return weekdays[weekday]
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  loadTemplates()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  loadTemplates()
}
</script>

<style scoped>
.time-slot-template-manage {
  background-color: transparent;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 30px;
}

.time-slot-template-manage::before {
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

.create-btn {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.create-btn:hover {
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

.template-table {
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}

.template-table th {
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  font-weight: 600;
  color: #333;
}

.template-table tr:hover {
  background-color: rgba(102, 126, 234, 0.05);
  transition: background-color 0.3s ease;
}

.edit-btn {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 6px;
  transition: all 0.3s ease;
  margin-right: 8px;
}

.edit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.delete-btn {
  border-radius: 6px;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(245, 101, 101, 0.3);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
  padding: 0 20px 20px;
}

.template-dialog {
  border-radius: 16px;
  overflow: hidden;
}

.template-dialog .el-dialog__header {
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  padding: 20px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
}

.template-dialog .el-dialog__title {
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
  width: 100%;
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

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .time-slot-template-manage { padding: 20px; }
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