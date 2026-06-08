<template>
  <div class="resource-manage">
    <h2 class="page-title">可预约资源配置</h2>

    <el-card class="info-card" shadow="hover">
      <div class="info-content">
        <el-icon><InfoFilled /></el-icon>
        <span>配置每周各天的可预约时间段和业务类型。点击编辑按钮可修改单个工作日的配置。</span>
      </div>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <el-table :data="resources" style="width: 100%" class="resource-table" row-key="id">
        <el-table-column prop="dayName" label="星期" width="120" align="center">
          <template #default="scope">
            <span class="day-name">{{ scope.row.dayName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="isWorkday" label="类型" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.isWorkday === 1 ? 'success' : 'warning'" size="small">
              {{ scope.row.isWorkday === 1 ? '工作日' : '休息日' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="timeSlots" label="时间段配置" align="center">
          <template #default="scope">
            <div class="time-slots-display" v-if="parseTimeSlots(scope.row.timeSlots).length">
              <div v-for="(slot, idx) in parseTimeSlots(scope.row.timeSlots)" :key="idx" class="time-slot-item">
                <span class="time-range">{{ slot.startTime }} - {{ slot.endTime }}</span>
                <span class="quota-badge">名额: {{ slot.maxQuota }}</span>
              </div>
            </div>
            <span v-else class="no-data">休息</span>
          </template>
        </el-table-column>
        <el-table-column prop="availableBusinessTypes" label="可办理业务" align="center">
          <template #default="scope">
            <div class="business-types" v-if="parseBusinessTypes(scope.row.availableBusinessTypes).length">
              <el-tag
                v-for="(type, idx) in parseBusinessTypes(scope.row.availableBusinessTypes)"
                :key="idx"
                size="small"
                style="margin: 2px;"
              >
                {{ type }}
              </el-tag>
            </div>
            <span v-else class="no-data">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="openEditDialog(scope.row)"
              :disabled="scope.row.isWorkday === 0"
            >
              编辑
            </el-button>
            <el-button
              :type="scope.row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑资源配置"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="120px">
        <el-form-item label="星期">
          <el-input v-model="editForm.dayName" disabled />
        </el-form-item>
        <el-form-item label="是否为工作日">
          <el-switch v-model="editForm.isWorkday" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-form-item label="时间段配置" v-if="editForm.isWorkday === 1">
          <div class="time-slots-editor">
            <div v-for="(slot, idx) in editForm.timeSlots" :key="idx" class="time-slot-edit-item">
              <el-time-select
                v-model="slot.startTime"
                placeholder="开始时间"
                start="08:00"
                step="00:30"
                end="20:00"
                style="width: 120px; margin-right: 10px"
              />
              <span>至</span>
              <el-time-select
                v-model="slot.endTime"
                placeholder="结束时间"
                start="08:00"
                step="00:30"
                end="20:00"
                style="width: 120px; margin-left: 10px; margin-right: 10px"
              />
              <el-input-number
                v-model="slot.maxQuota"
                :min="1"
                :max="100"
                controls-position="right"
                style="width: 100px; margin-right: 5px"
              />
              <el-button type="danger" circle @click="removeTimeSlot(idx)" :icon="Delete" />
            </div>
            <el-button type="primary" plain @click="addTimeSlot" class="add-slot-btn">
              <el-icon><Plus /></el-icon> 添加时间段
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="可办理业务" v-if="editForm.isWorkday === 1">
          <el-checkbox-group v-model="editForm.selectedBusinessTypes">
            <el-checkbox label="开户" value="开户" />
            <el-checkbox label="销户" value="销户" />
            <el-checkbox label="缴费" value="缴费" />
            <el-checkbox label="咨询" value="咨询" />
            <el-checkbox label="变更" value="变更" />
            <el-checkbox label="其他" value="其他" />
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="备注描述">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="2"
            placeholder="可选填写备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveResource">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled, Plus, Delete } from '@element-plus/icons-vue'
import { resourceApi } from '../../api'

const resources = ref([])
const editDialogVisible = ref(false)
const editForm = ref({
  id: null,
  dayName: '',
  isWorkday: 1,
  timeSlots: [],
  selectedBusinessTypes: [],
  description: '',
  status: 1
})

onMounted(() => {
  fetchResources()
})

const fetchResources = async () => {
  try {
    const res = await resourceApi.list()
    if (res.code === 200) {
      resources.value = res.data
    }
  } catch (error) {
    console.error('获取资源配置失败:', error)
    ElMessage.error('获取资源配置失败')
  }
}

const parseTimeSlots = (jsonStr) => {
  if (!jsonStr) return []
  try {
    return JSON.parse(jsonStr)
  } catch {
    return []
  }
}

const parseBusinessTypes = (jsonStr) => {
  if (!jsonStr) return []
  try {
    return JSON.parse(jsonStr)
  } catch {
    return []
  }
}

const openEditDialog = (row) => {
  editForm.value = {
    id: row.id,
    dayName: row.dayName,
    isWorkday: row.isWorkday,
    timeSlots: parseTimeSlots(row.timeSlots),
    selectedBusinessTypes: parseBusinessTypes(row.availableBusinessTypes),
    description: row.description || '',
    status: row.status
  }
  editDialogVisible.value = true
}

const addTimeSlot = () => {
  editForm.value.timeSlots.push({
    startTime: '09:00',
    endTime: '12:00',
    maxQuota: 20
  })
}

const removeTimeSlot = (index) => {
  editForm.value.timeSlots.splice(index, 1)
}

const saveResource = async () => {
  // 校验：工作日有时段时业务类型不能为空
  if (editForm.value.isWorkday === 1 && editForm.value.timeSlots.length > 0 && editForm.value.selectedBusinessTypes.length === 0) {
    ElMessage.warning('有时段配置时，可办理业务不能为空')
    return
  }

  const data = {
    id: editForm.value.id,
    dayOfWeek: resources.value.find(r => r.id === editForm.value.id)?.dayOfWeek,
    dayName: editForm.value.dayName,
    isWorkday: editForm.value.isWorkday,
    timeSlots: JSON.stringify(editForm.value.timeSlots),
    availableBusinessTypes: JSON.stringify(editForm.value.selectedBusinessTypes),
    description: editForm.value.description,
    status: editForm.value.status
  }

  try {
    const res = await resourceApi.update(data)
    if (res.code === 200) {
      ElMessage.success(res.message || '保存成功')
      editDialogVisible.value = false
      fetchResources()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

const toggleStatus = async (row) => {
  try {
    const res = await resourceApi.toggleStatus(row.id)
    if (res.code === 200) {
      ElMessage.success(res.message || `已${row.status === 1 ? '禁用' : '启用'}`)
      fetchResources()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('状态切换失败:', error)
    ElMessage.error('状态切换失败')
  }
}
</script>

<style scoped>
.resource-manage {
  background-color: transparent;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 30px;
}

.resource-manage::before {
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

.info-card {
  margin-bottom: 20px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e3f2fd, #f3e5f5);
  border: 1px solid rgba(102, 126, 234, 0.2);
  position: relative;
  z-index: 1;
}

.info-content {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #666;
  font-size: 14px;
}

.info-content .el-icon {
  color: #667eea;
  font-size: 18px;
}

.table-card {
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

.resource-table {
  border-radius: 16px 16px 0 0;
}

.day-name {
  font-weight: 600;
  color: #333;
}

.time-slots-display {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.time-slot-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-range {
  color: #666;
  font-size: 13px;
}

.quota-badge {
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.business-types {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
}

.no-data {
  color: #999;
  font-style: italic;
}

.time-slots-editor {
  width: 100%;
}

.time-slot-edit-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  gap: 5px;
}

.add-slot-btn {
  margin-top: 10px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .resource-manage {
    padding: 20px;
  }
  .page-title {
    font-size: 1.8rem;
  }
  .time-slot-edit-item {
    flex-wrap: wrap;
  }
}
</style>
