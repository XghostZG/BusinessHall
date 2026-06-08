<template>
  <div class="approval-center">
    <h2 class="page-title">审批中心</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon pending">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pending }}</div>
              <div class="stat-label">待审批</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon approved">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.approved }}</div>
              <div class="stat-label">已通过</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon rejected">
              <el-icon><CircleClose /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.rejected }}</div>
              <div class="stat-label">已拒绝</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">全部申请</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选和搜索 -->
    <el-card class="filter-card" shadow="hover">
      <div class="filter-form">
        <el-form :inline="true" :model="filterForm">
          <el-form-item label="申请类型">
            <el-select v-model="filterForm.type" placeholder="全部类型" style="width: 140px" clearable>
              <el-option label="请假申请" value="leave" />
              <el-option label="投诉申请" value="complaint" />
            </el-select>
          </el-form-item>
          <el-form-item label="审批状态">
            <el-select v-model="filterForm.status" placeholder="全部状态" style="width: 140px" clearable>
              <el-option label="待审批" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已拒绝" value="rejected" />
            </el-select>
          </el-form-item>
          <el-form-item label="申请人">
            <el-input v-model="filterForm.applicant" placeholder="请输入申请人" style="width: 180px" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleFilter" :icon="Search">查询</el-button>
            <el-button @click="resetFilter" :icon="RefreshRight">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 申请列表 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="table-header">
          <span class="table-title">申请列表</span>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="pending">待审批</el-radio-button>
            <el-radio-button label="all">全部记录</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="filteredApplications" style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="申请类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" effect="plain">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="120" align="center" />
        <el-table-column label="申请内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.type === 'leave'">
              {{ row.title }}：{{ row.content?.substring(0, 50) }}{{ row.content?.length > 50 ? '...' : '' }}
            </span>
            <span v-else-if="row.type === 'complaint'">
              {{ row.title }}：{{ row.content?.substring(0, 30) }}{{ row.content?.length > 30 ? '...' : '' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" effect="light">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              type="primary"
              link
              size="small"
              @click="handleApprove(row)"
            >
              审批
            </el-button>
            <el-button
              v-else
              type="info"
              link
              size="small"
              @click="handleView(row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 审批对话框 - 请假申请 -->
    <el-dialog
      v-model="leaveDialogVisible"
      title="请假审批"
      width="600px"
      destroy-on-close
    >
      <div v-if="currentApplication && currentApplication.type === 'leave'" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请人">{{ currentApplication.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="申请标题">{{ currentApplication.title }}</el-descriptions-item>
          <el-descriptions-item label="申请时间" :span="2">
            {{ formatDateTime(currentApplication.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <h4>申请内容</h4>
          <div class="reason-box">{{ currentApplication.content }}</div>
        </div>

        <el-divider />

        <el-form :model="approveForm" label-width="100px">
          <el-form-item label="审批结果" required>
            <el-radio-group v-model="approveForm.result">
              <el-radio label="approved">批准</el-radio>
              <el-radio label="rejected">拒绝</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审批备注">
            <el-input
              v-model="approveForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入审批备注（可选）"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="leaveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLeaveApproval" :loading="submitting">
          提交审批
        </el-button>
      </template>
    </el-dialog>

    <!-- 审批对话框 - 投诉申请 -->
    <el-dialog
      v-model="complaintDialogVisible"
      title="投诉处理"
      width="700px"
      destroy-on-close
    >
      <div v-if="currentApplication && currentApplication.type === 'complaint'" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="投诉编号">{{ currentApplication.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="投诉人">{{ currentApplication.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="申请标题" :span="2">
            {{ currentApplication.title }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">
            {{ formatDateTime(currentApplication.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <h4>投诉原因</h4>
          <div class="reason-box complaint">{{ currentApplication.content }}</div>
        </div>

        <div v-if="currentApplication.type === 'complaint'" class="detail-section">
          <h4>额外信息</h4>
          <el-descriptions :column="1" border size="small">
            <template v-for="item in parseExtraData(currentApplication.extraData)" :key="item.label">
              <el-descriptions-item :label="item.label">
                <div v-if="item.isImages" class="evidence-images">
                  <div v-for="(url, index) in item.value.urls" :key="index" class="image-wrapper">
                    <el-image 
                      :src="url" 
                      :preview-src-list="item.value.urls"
                      fit="cover"
                      class="evidence-image"
                    />
                  </div>
                </div>
                <span v-else>{{ item.value }}</span>
              </el-descriptions-item>
            </template>
          </el-descriptions>
        </div>

        <el-divider />

        <el-form :model="decisionForm" label-width="100px">
          <el-form-item label="最终决策" required>
            <el-radio-group v-model="decisionForm.decision">
              <el-radio label="approved">接受投诉</el-radio>
              <el-radio label="rejected">驳回投诉</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="反馈内容" required>
            <el-input
              v-model="decisionForm.feedback"
              type="textarea"
              :rows="4"
              placeholder="请输入反馈给用户的内容..."
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="complaintDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplaintDecision" :loading="submitting">
          提交反馈
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="申请详情"
      width="600px"
      destroy-on-close
    >
      <div v-if="currentApplication" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请类型">
            {{ getTypeLabel(currentApplication.type) }}
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusTagType(currentApplication.status)">
              {{ getStatusLabel(currentApplication.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentApplication.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">
            {{ formatDateTime(currentApplication.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 请假详情 -->
        <template v-if="currentApplication.type === 'leave'">
          <div class="detail-section">
            <h4>请假信息</h4>
            <p><strong>申请标题：</strong>{{ currentApplication.title }}</p>
            <p><strong>申请内容：</strong>{{ currentApplication.content }}</p>
          </div>
        </template>

        <!-- 投诉详情 -->
        <template v-if="currentApplication.type === 'complaint'">
          <div class="detail-section">
            <h4>投诉信息</h4>
            <p><strong>投诉编号：</strong>{{ currentApplication.applicationNo }}</p>
            <p><strong>申请标题：</strong>{{ currentApplication.title }}</p>
            <p><strong>投诉原因：</strong>{{ currentApplication.content }}</p>
          </div>
          <div v-if="currentApplication.type === 'complaint'" class="detail-section">
            <h4>额外信息</h4>
            <el-descriptions :column="1" border size="small">
              <template v-for="item in parseExtraData(currentApplication.extraData)" :key="item.label">
                <el-descriptions-item :label="item.label">
                  <div v-if="item.isImages" class="evidence-images">
                    <div v-for="(url, index) in item.value.urls" :key="index" class="image-wrapper">
                      <el-image 
                        :src="url" 
                        :preview-src-list="item.value.urls"
                        fit="cover"
                        class="evidence-image"
                      />
                    </div>
                  </div>
                  <span v-else>{{ item.value }}</span>
                </el-descriptions-item>
              </template>
            </el-descriptions>
          </div>
        </template>

        <!-- 审批结果 -->
        <div v-if="currentApplication.status !== 'pending'" class="detail-section">
          <h4>审批结果</h4>
          <p><strong>审批结果：</strong>{{ currentApplication.approveResult || currentApplication.finalDecision }}</p>
          <p v-if="currentApplication.approveRemark || currentApplication.feedbackToUser">
            <strong>审批备注：</strong>{{ currentApplication.approveRemark || currentApplication.feedbackToUser }}
          </p>
          <p v-if="currentApplication.approveTime || currentApplication.decisionTime">
            <strong>审批时间：</strong>{{ formatDateTime(currentApplication.approveTime || currentApplication.decisionTime) }}
          </p>
        </div>
      </div>

      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, CircleCheck, CircleClose, Document, Search, RefreshRight } from '@element-plus/icons-vue'
import { approvalApi } from '../../api'
import { useUserStore } from '../../store/user'
import { getImageUrl } from '../../config'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const applications = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const viewMode = ref('pending')

// 对话框显示状态
const leaveDialogVisible = ref(false)
const complaintDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const currentApplication = ref(null)

// 统计
const stats = reactive({
  pending: 0,
  approved: 0,
  rejected: 0,
  total: 0
})

// 筛选表单
const filterForm = reactive({
  type: '',
  status: '',
  applicant: ''
})

// 审批表单
const approveForm = reactive({
  result: 'approved',
  remark: ''
})

// 决策表单
const decisionForm = reactive({
  decision: 'approved',
  feedback: ''
})

// 获取申请列表
const fetchApplications = async () => {
  loading.value = true
  try {
    // 使用新的统一审批API获取数据
    const res = await approvalApi.getList({
      type: filterForm.type || undefined,
      status: filterForm.status || undefined
    })

    // 转换数据为统一格式
    applications.value = (res || []).map(item => ({
      id: item.id,
      type: item.applicationType,
      sourceId: item.sourceId,
      applicantId: item.applicantId,
      applicantName: item.applicantName,
      applicationNo: item.applicationNo,
      title: item.title,
      content: item.content,
      status: item.status,
      approverId: item.approverId,
      approveTime: item.approveTime,
      approveResult: item.approveResult,
      approveRemark: item.approveRemark,
      createTime: item.createTime,
      updateTime: item.updateTime,
      extraData: item.extraData
    }))

    // 更新统计
    await fetchStatistics()
  } catch (error) {
    console.error('获取申请列表失败:', error)
    ElMessage.error('获取申请列表失败')
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const res = await approvalApi.getStatistics()
    if (res) {
      stats.pending = res.pending || 0
      stats.approved = res.approved || 0
      stats.rejected = res.rejected || 0
      stats.total = res.total || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}



// 过滤后的申请列表
const filteredApplications = computed(() => {
  let result = applications.value

  // 根据视图模式过滤
  if (viewMode.value === 'pending') {
    result = result.filter(a => a.status === 'pending')
  }

  // 根据筛选条件过滤
  if (filterForm.type) {
    result = result.filter(a => a.type === filterForm.type)
  }
  if (filterForm.status) {
    result = result.filter(a => a.status === filterForm.status)
  }
  if (filterForm.applicant) {
    result = result.filter(a =>
      a.applicantName?.includes(filterForm.applicant)
    )
  }

  // 分页
  total.value = result.length
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return result.slice(start, end)
})

// 类型标签
const getTypeLabel = (type) => {
  const map = { leave: '请假申请', complaint: '投诉申请' }
  return map[type] || type
}

// 类型标签样式
const getTypeTagType = (type) => {
  const map = { leave: 'primary', complaint: 'warning' }
  return map[type] || ''
}

// 状态标签
const getStatusLabel = (status) => {
  const map = { pending: '待审批', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

// 状态标签样式
const getStatusTagType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

// 解析额外信息（后端已解析为中文键值对）
const parseExtraData = (extraData) => {
  try {
    const data = typeof extraData === 'string' ? JSON.parse(extraData) : extraData
    if (!data || typeof data !== 'object') return []
    return Object.entries(data)
      .map(([key, value]) => {
        let label = key
        let displayValue = value
        const isEvidenceField = key === 'evidence' || key === '证据描述'
        
        // 转换字段名为中文
        const labelMap = {
          appointmentId: '关联预约ID',
          complainedUserId: '被投诉人ID',
          evidence: '证据图片',
        }
        label = labelMap[key] || key
        
        // 处理证据图片（支持英文键名和中文键名）
        if (isEvidenceField && value && value !== '') {
          const urls = value.split(',').filter(url => url.trim())
          if (urls.length > 0) {
            // 使用 getImageUrl 确保图片路径正确
            const fullUrls = urls.map(url => getImageUrl(url))
            displayValue = {
              type: 'images',
              urls: fullUrls
            }
          } else {
            displayValue = '-'
          }
        } else if (value === null || value === undefined || value === '') {
          displayValue = '-'
        } else if (typeof value === 'object') {
          displayValue = JSON.stringify(value)
        } else {
          displayValue = String(value)
        }
        
        return {
          label,
          value: displayValue,
          isImages: isEvidenceField && typeof displayValue === 'object' && displayValue.type === 'images'
        }
      })
  } catch {
    return []
  }
}

// 格式化日期时间
const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  const d = new Date(datetime)
  return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

// 处理审批
const handleApprove = (row) => {
  currentApplication.value = row
  if (row.type === 'leave') {
    approveForm.result = 'approved'
    approveForm.remark = ''
    leaveDialogVisible.value = true
  } else if (row.type === 'complaint') {
    decisionForm.decision = 'approved'
    decisionForm.feedback = ''
    complaintDialogVisible.value = true
  }
}

// 查看详情
const handleView = (row) => {
  currentApplication.value = row
  viewDialogVisible.value = true
}

// 提交请假审批
const submitLeaveApproval = async () => {
  if (!currentApplication.value) return

  submitting.value = true
  try {
    // 使用统一审批API
    const res = await approvalApi.approve(
      currentApplication.value.id,
      approveForm.result,
      approveForm.remark
    )
    if (res.code === 200) {
      ElMessage.success('审批成功')
      leaveDialogVisible.value = false
      fetchApplications()
    } else {
      ElMessage.error(res.message || '审批失败')
    }
  } catch (error) {
    console.error('审批失败:', error)
    ElMessage.error('审批失败')
  } finally {
    submitting.value = false
  }
}

// 提交投诉决策
const submitComplaintDecision = async () => {
  if (!currentApplication.value) return
  if (!decisionForm.feedback.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }

  submitting.value = true
  try {
    // 使用统一审批API
    const result = decisionForm.decision === 'reprocess' ? 'rejected' : decisionForm.decision
    const res = await approvalApi.approve(
      currentApplication.value.id,
      result,
      decisionForm.feedback
    )
    if (res.code === 200) {
      ElMessage.success('决策已提交')
      complaintDialogVisible.value = false
      fetchApplications()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (error) {
    console.error('提交反馈失败:', error)
    ElMessage.error('提交反馈失败')
  } finally {
    submitting.value = false
  }
}

// 筛选
const handleFilter = () => {
  currentPage.value = 1
}

// 重置筛选
const resetFilter = () => {
  filterForm.type = ''
  filterForm.status = ''
  filterForm.applicant = ''
  currentPage.value = 1
}

// 分页
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

onMounted(() => {
  fetchApplications()
})
</script>

<style scoped>
.approval-center {
  padding: 30px;
  min-height: 100%;
}

.page-title {
  text-align: center;
  color: #333;
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 30px;
  position: relative;
}

.page-title::after {
  content: '';
  display: block;
  width: 100px;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  margin: 15px auto 0;
  border-radius: 2px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.pending {
  background: #fdf6ec;
  color: #e6a23c;
}

.stat-icon.approved {
  background: #f0f9eb;
  color: #67c23a;
}

.stat-icon.rejected {
  background: #fef0f0;
  color: #f56c6c;
}

.stat-icon.total {
  background: #ecf5ff;
  color: #409eff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.filter-form {
  padding: 10px;
}

.table-card {
  border-radius: 12px;
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

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-content {
  padding: 10px 0;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  font-size: 14px;
  color: #333;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.reason-box {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  color: #666;
  line-height: 1.8;
}

.reason-box.complaint {
  background: #fef0f0;
}

.evidence-box {
  background: #ecf5ff;
  padding: 15px;
  border-radius: 8px;
  color: #409eff;
  line-height: 1.6;
}

.evidence-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 0;
}

.image-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.evidence-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.handle-result-box {
  background: #f0f9eb;
  padding: 15px;
  border-radius: 8px;
}

.handle-time {
  font-size: 12px;
  color: #999;
  margin-top: 10px;
  text-align: right;
}
</style>
