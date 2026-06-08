<template>
  <div class="consultation-page">
    <div class="page-header">
      <h2>客服咨询</h2>
      <p class="subtitle">遇到问题？我们随时为您服务</p>
    </div>

    <div class="consultation-form">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>提交咨询</span>
            <el-button text @click="$router.push('/my-consultations')">
              我的咨询记录
            </el-button>
          </div>
        </template>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
          <el-form-item label="咨询标题" prop="title">
            <el-input 
              v-model="form.title" 
              placeholder="请输入咨询标题"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="咨询内容" prop="content">
            <el-input 
              v-model="form.content" 
              type="textarea"
              :rows="6"
              placeholder="请详细描述您的问题..."
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submitForm" :loading="submitting">
              提交咨询
            </el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="info-card" shadow="never">
        <template #header>
          <span>咨询须知</span>
        </template>
        <ul class="info-list">
          <li>工作时间：周一至周五 9:00-18:00</li>
          <li>我们将尽快在24小时内回复您的咨询</li>
          <li>紧急问题可拨打客服热线：400-xxx-xxxx</li>
          <li>提交后可在"我的咨询记录"中查看回复</li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { consultationApi } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  title: '',
  content: ''
})

const rules = {
  title: [
    { required: true, message: '请输入咨询标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度在 5 到 100 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入咨询内容', trigger: 'blur' },
    { min: 10, max: 1000, message: '内容长度在 10 到 1000 个字符', trigger: 'blur' }
  ]
}

const submitForm = async () => {
  if (!formRef.value) return
  
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    await consultationApi.create({
      userId: userStore.user?.id,
      title: form.title,
      content: form.content
    })
    ElMessage.success('提交成功')
    resetForm()
    setTimeout(() => {
      router.push('/my-consultations')
    }, 1500)
  } catch (error) {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
}
</script>

<style scoped>
.consultation-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 8px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.consultation-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-card {
  background: #f5f7fa;
  border: none;
}

.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.info-list li {
  padding: 8px 0;
  color: #666;
  font-size: 14px;
  position: relative;
  padding-left: 20px;
}

.info-list li::before {
  content: "•";
  position: absolute;
  left: 0;
  color: #409eff;
}
</style>
