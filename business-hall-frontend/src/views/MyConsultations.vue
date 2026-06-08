<template>
  <div class="my-consultations-page">
    <div class="page-header">
      <h2>我的咨询</h2>
      <el-button type="primary" @click="$router.push('/consultation')">
        新建咨询
      </el-button>
    </div>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>咨询记录</span>
          <el-tag v-if="consultations.length > 0" type="info">
            共 {{ consultations.length }} 条
          </el-tag>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-empty v-else-if="consultations.length === 0" description="暂无咨询记录">
        <el-button type="primary" @click="$router.push('/consultation')">
          立即咨询
        </el-button>
      </el-empty>

      <div v-else class="consultation-list">
        <el-timeline>
          <el-timeline-item
            v-for="item in consultations"
            :key="item.id"
            :timestamp="formatTime(item.createTime)"
            placement="top"
            :type="getStatusType(item.status)"
          >
            <el-card shadow="hover" class="consultation-item">
              <template #header>
                <div class="item-header">
                  <span class="consultation-no">{{ item.consultationNo }}</span>
                  <el-tag :type="getStatusTagType(item.status)" size="small">
                    {{ item.status }}
                  </el-tag>
                </div>
                <h4 class="consultation-title">{{ item.title }}</h4>
              </template>
              
              <div class="consultation-content">
                <p class="content-label">我的问题：</p>
                <p class="content-text">{{ item.content }}</p>
              </div>

              <div v-if="item.replyContent" class="consultation-reply">
                <el-divider content-position="left">
                  <span class="reply-label">客服回复</span>
                </el-divider>
                <p class="content-text">{{ item.replyContent }}</p>
                <p class="reply-time">回复时间：{{ formatTime(item.replyTime) }}</p>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { consultationApi } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const loading = ref(false)
const consultations = ref([])

const loadConsultations = async () => {
  loading.value = true
  try {
    consultations.value = await consultationApi.getMyList(userStore.user?.id)
  } catch (error) {
    console.error('加载咨询记录失败', error)
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

const getStatusType = (status) => {
  const map = {
    '待回复': 'warning',
    '处理中': 'primary',
    '已回复': 'success'
  }
  return map[status] || 'info'
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
.my-consultations-page {
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

.consultation-list {
  padding: 10px 0;
}

.consultation-item {
  margin-bottom: 10px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.consultation-no {
  font-size: 12px;
  color: #999;
}

.consultation-title {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.consultation-content {
  margin-top: 10px;
}

.content-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.content-text {
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.consultation-reply {
  margin-top: 15px;
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
}

.reply-label {
  font-size: 12px;
  color: #409eff;
  font-weight: bold;
}

.reply-time {
  font-size: 12px;
  color: #999;
  margin-top: 10px;
  text-align: right;
}
</style>
