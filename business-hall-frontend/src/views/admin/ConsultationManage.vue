<template>
  <div class="consultation-manage-page">
    <div class="page-header">
      <h2>咨询管理</h2>
    </div>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>全部咨询记录</span>
          <el-statistic title="待处理" :value="pendingCount">
            <template #suffix>
              <span style="font-size: 14px">条</span>
            </template>
          </el-statistic>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-empty v-else-if="consultations.length === 0" description="暂无咨询记录" />

      <el-table v-else :data="consultations" stripe style="width: 100%">
        <el-table-column prop="consultationNo" label="咨询编号" width="180" />
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column prop="userName" label="咨询人" width="100" />
        <el-table-column prop="userPhone" label="联系电话" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handlerName" label="处理人" width="100">
          <template #default="{ row }">
            {{ row.handlerName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="咨询详情" width="700px">
      <div v-if="currentConsultation" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="咨询编号">
            {{ currentConsultation.consultationNo }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(currentConsultation.status)" size="small">
              {{ currentConsultation.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="咨询人">
            {{ currentConsultation.userName }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ currentConsultation.userPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">
            {{ formatTime(currentConsultation.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <h4>咨询标题</h4>
          <p>{{ currentConsultation.title }}</p>
        </div>

        <div class="detail-section">
          <h4>咨询内容</h4>
          <p class="content-text">{{ currentConsultation.content }}</p>
        </div>

        <div v-if="currentConsultation.replyContent" class="detail-section reply-section">
          <h4>回复内容</h4>
          <p class="content-text">{{ currentConsultation.replyContent }}</p>
          <p class="reply-info">
            回复人：{{ currentConsultation.handlerName }} | 
            回复时间：{{ formatTime(currentConsultation.replyTime) }}
          </p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { consultationApi } from '../../api'

const loading = ref(false)
const consultations = ref([])
const detailVisible = ref(false)
const currentConsultation = ref(null)

const pendingCount = computed(() =>
  consultations.value.filter(c => c.status === '待回复' || c.status === '处理中').length
)

const loadConsultations = async () => {
  loading.value = true
  try {
    const res = await consultationApi.list()
    if (res.code === 200) {
      consultations.value = res.data
    }
  } catch (error) {
    console.error('加载咨询列表失败', error)
  } finally {
    loading.value = false
  }
}

const viewDetail = async (row) => {
  try {
    const res = await consultationApi.getDetail(row.id)
    if (res.code === 200) {
      currentConsultation.value = res.data
      detailVisible.value = true
    }
  } catch (error) {
    console.error('加载详情失败', error)
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
    '待回复': 'warning',
    '处理中': 'primary',
    '已回复': 'success'
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadConsultations()
})
</script>

<style scoped>
.consultation-manage-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  padding: 40px;
  color: #666;
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
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.detail-section p {
  color: #666;
  line-height: 1.8;
  margin: 0;
}

.reply-section {
  background: #f0f9eb;
  padding: 15px;
  border-radius: 4px;
}

.reply-info {
  font-size: 12px;
  color: #999;
  margin-top: 10px;
  text-align: right;
}
</style>
