<template>
  <div class="my-complaints-page">
    <div class="page-header">
      <h2>我的投诉</h2>
      <el-button type="primary" @click="$router.push('/complaint')">
        新建投诉
      </el-button>
    </div>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>投诉记录</span>
          <el-tag v-if="complaints.length > 0" type="info">
            共 {{ complaints.length }} 条
          </el-tag>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-empty v-else-if="complaints.length === 0" description="暂无投诉记录">
        <el-button type="primary" @click="$router.push('/complaint')">
          立即投诉
        </el-button>
      </el-empty>

      <div v-else class="complaint-list">
        <el-card
          v-for="item in complaints"
          :key="item.id"
          class="complaint-item"
          shadow="hover"
        >
          <template #header>
            <div class="item-header">
              <div class="item-info">
                <span class="complaint-no">{{ item.complaintNo }}</span>
                <span v-if="item.appointmentId" class="related-appointment">
                  关联预约: AP{{ item.appointmentId }}
                </span>
              </div>
              <el-tag :type="getStatusTagType(item.status)" size="small">
                {{ item.status }}
              </el-tag>
            </div>
          </template>

          <div class="complaint-content">
            <div class="content-section">
              <p class="section-label">投诉原因</p>
              <p class="section-text">{{ item.reason }}</p>
            </div>

            <div v-if="item.complainedUserName" class="content-section">
              <p class="section-label">被投诉人</p>
              <p class="section-text">{{ item.complainedUserName }}</p>
            </div>

            <div v-if="item.evidence" class="content-section">
              <p class="section-label">证据</p>
              <p class="section-text evidence-text">{{ item.evidence }}</p>
            </div>
          </div>

          <el-divider v-if="item.handleResult || item.finalDecision" />

          <div v-if="item.handleResult" class="handle-section">
            <p class="section-label">
              <el-icon><SuccessFilled /></el-icon>
              营业员处理结果
            </p>
            <p class="section-text">{{ item.handleResult }}</p>
            <p class="section-time">处理时间：{{ formatTime(item.handleTime) }}</p>
          </div>

          <div v-if="item.feedbackToUser" class="feedback-section">
            <p class="section-label">
              <el-icon><CircleCheckFilled /></el-icon>
              管理员最终反馈
            </p>
            <p class="section-text">{{ item.feedbackToUser }}</p>
            <p class="section-time">反馈时间：{{ formatTime(item.feedbackTime) }}</p>
          </div>

          <div class="item-footer">
            <span class="create-time">提交时间：{{ formatTime(item.createTime) }}</span>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading, SuccessFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import { approvalApi } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const loading = ref(false)
const complaints = ref([])

const loadComplaints = async () => {
  loading.value = true
  try {
    const res = await approvalApi.getMyList({ type: 'complaint' })
    // 转换统一申请格式为投诉显示格式
    complaints.value = (res || []).map(item => {
      const extraData = item.extraData ? JSON.parse(item.extraData) : {}
      return {
        id: item.id,
        complaintNo: item.applicationNo,
        appointmentId: extraData.appointmentId,
        complainedUserId: extraData.complainedUserId,
        reason: item.content,
        evidence: extraData.evidence,
        status: item.status === 'pending' ? '待处理' :
                item.status === 'approved' ? '已反馈' : '已驳回',
        handleResult: item.approveResult,
        feedbackToUser: item.approveRemark,
        feedbackTime: item.approveTime,
        createTime: item.createTime
      }
    })
  } catch (error) {
    console.error('加载投诉记录失败', error)
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusTagType = (status) => {
  const map = {
    '待处理': 'warning',
    '处理中': 'primary',
    '已处理': 'info',
    '已反馈': 'success',
    '已驳回': 'danger'
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadComplaints()
})
</script>

<style scoped>
.my-complaints-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  padding: 40px;
  color: #666;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.complaint-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.complaint-item {
  border-left: 3px solid var(--el-color-warning);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.complaint-no {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.related-appointment {
  font-size: 12px;
  color: #999;
}

.complaint-content {
  margin-top: 15px;
}

.content-section {
  margin-bottom: 12px;
}

.section-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.section-text {
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.evidence-text {
  color: #409eff;
}

.section-time {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  text-align: right;
}

.handle-section,
.feedback-section {
  margin-top: 15px;
  padding: 15px;
  border-radius: 4px;
}

.handle-section {
  background: #ecf5ff;
}

.feedback-section {
  background: #f0f9eb;
}

.item-footer {
  margin-top: 15px;
  text-align: right;
}

.create-time {
  font-size: 12px;
  color: #999;
}
</style>
