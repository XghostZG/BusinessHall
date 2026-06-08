<template>
  <div class="complaint-page">
    <div class="page-header">
      <h2>投诉建议</h2>
      <p class="subtitle">您的反馈是我们改进的动力</p>
    </div>

    <div class="complaint-form">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>提交投诉</span>
            <el-button text @click="$router.push('/my-complaints')">
              我的投诉记录
            </el-button>
          </div>
        </template>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
          <el-form-item label="关联预约" prop="appointmentId">
            <el-select 
              v-model="form.appointmentId" 
              placeholder="请选择关联的预约（可选）"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in appointments"
                :key="item.id"
                :label="`${item.appointmentNo || 'AP' + item.id} - ${item.businessType}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="被投诉营业员" prop="complainedUserId">
            <el-select
              v-model="form.complainedUserId"
              placeholder="请选择被投诉的营业员（可选）"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in staffList"
                :key="item.id"
                :label="item.realName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="投诉原因" prop="reason">
            <el-input
              v-model="form.reason"
              type="textarea"
              :rows="5"
              placeholder="请详细描述您投诉的原因..."
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="上传证据">
            <el-upload
              action="#"
              :auto-upload="false"
              :limit="3"
              accept="image/*"
              list-type="picture-card"
              :on-change="handleFileChange"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">最多上传3张图片作为证据</div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submitForm" :loading="submitting">
              提交投诉
            </el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="info-card" shadow="never">
        <template #header>
          <span>投诉须知</span>
        </template>
        <ul class="info-list">
          <li>请确保投诉内容真实有效</li>
          <li>我们将在1-3个工作日内处理您的投诉</li>
          <li>投诉处理结果可在"我的投诉记录"中查看</li>
          <li>如有紧急情况，请拨打客服热线</li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { approvalApi, appointmentApi, userApi } from '../api'
import { useUserStore } from '../store/user'
import { config } from '../config'
import api from '../utils/request'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)
const appointments = ref([])
const staffList = ref([])
const evidenceFiles = ref([])
const uploadUrls = ref([])

const form = reactive({
  appointmentId: null,
  complainedUserId: null,
  reason: ''
})

const rules = {
  reason: [
    { required: true, message: '请输入投诉原因', trigger: 'blur' },
    { min: 10, max: 1000, message: '内容长度在 10 到 1000 个字符', trigger: 'blur' }
  ]
}

const loadAppointments = async () => {
  try {
    appointments.value = await appointmentApi.getMyList(userStore.user?.id)
  } catch (error) {
    console.error('加载预约记录失败', error)
  }
}

const loadStaffList = async () => {
  try {
    const list = await userApi.list()
    staffList.value = list.filter(u => u.role === 'clerk')
  } catch (error) {
    console.error('加载营业员列表失败', error)
  }
}

// 上传单个文件 - 不显示消息，只返回结果或抛出异常，由调用方统一处理
const uploadFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file.raw)
  
  const result = await api.post('/upload/single', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  
  console.log('上传响应:', result)
  
  if (result.success) {
    // 返回完整的图片URL
    return config.apiBaseUrl + result.fileUrl
  } else {
    throw new Error(result.message || '图片上传失败')
  }
}

const handleFileChange = (_file, fileList) => {
  evidenceFiles.value = fileList
}

const submitForm = async () => {
  // 防重入保护
  if (submitting.value) return
  
  if (!formRef.value) return
  
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    // 先上传所有图片
    const uploadedUrls = []
    for (const file of evidenceFiles.value) {
      if (file.raw) {
        const url = await uploadFile(file)
        uploadedUrls.push(url)
      }
    }
    
    // 获取被投诉人姓名
    const complainedUser = staffList.value.find(u => u.id === form.complainedUserId)
    const complainedUserName = complainedUser ? complainedUser.realName : ''

    await approvalApi.submit({
      applicationType: 'complaint',
      title: '投诉' + (complainedUserName ? '-' + complainedUserName : ''),
      content: form.reason,
      extraData: JSON.stringify({
        appointmentId: form.appointmentId,
        complainedUserId: form.complainedUserId,
        evidence: uploadedUrls.join(',')
      })
    })

    ElMessage.success('提交成功')
    resetForm()
    setTimeout(() => {
      router.push('/my-complaints')
    }, 1500)

  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  evidenceFiles.value = []
  uploadUrls.value = []
}

onMounted(() => {
  loadAppointments()
  loadStaffList()
})
</script>

<style scoped>
.complaint-page {
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

.complaint-form {
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
  color: #e6a23c;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}
</style>
