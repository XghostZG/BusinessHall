<template>
  <div class="statistics">
    <h2 class="page-title">数据统计</h2>
    
    <div class="stats-container">
      <el-card class="stat-card" shadow="hover" :style="{ animationDelay: '0.1s' }">
        <div class="stat-icon">📊</div>
        <h3>今日预约总量</h3>
        <p class="stat-value">{{ todayTotal }}</p>
        <div :class="['stat-trend', trends.todayTotal.direction]">
          {{ trends.todayTotal.direction === 'positive' ? '↗' : trends.todayTotal.direction === 'negative' ? '↘' : '→' }} {{ trends.todayTotal.value }}
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover" :style="{ animationDelay: '0.2s' }">
        <div class="stat-icon">✅</div>
        <h3>已完成</h3>
        <p class="stat-value">{{ todayCompleted }}</p>
        <div :class="['stat-trend', trends.todayCompleted.direction]">
          {{ trends.todayCompleted.direction === 'positive' ? '↗' : trends.todayCompleted.direction === 'negative' ? '↘' : '→' }} {{ trends.todayCompleted.value }}
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover" :style="{ animationDelay: '0.3s' }">
        <div class="stat-icon">❌</div>
        <h3>已取消</h3>
        <p class="stat-value">{{ todayCanceled }}</p>
        <div :class="['stat-trend', trends.todayCanceled.direction]">
          {{ trends.todayCanceled.direction === 'positive' ? '↗' : trends.todayCanceled.direction === 'negative' ? '↘' : '→' }} {{ trends.todayCanceled.value }}
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover" :style="{ animationDelay: '0.4s' }">
        <div class="stat-icon">⏰</div>
        <h3>爽约</h3>
        <p class="stat-value">{{ todayNoShow }}</p>
        <div :class="['stat-trend', trends.todayNoShow.direction]">
          {{ trends.todayNoShow.direction === 'positive' ? '↗' : trends.todayNoShow.direction === 'negative' ? '↘' : '→' }} {{ trends.todayNoShow.value }}
        </div>
      </el-card>
    </div>
    
    <div class="stats-container">
      <el-card class="stat-card" shadow="hover" :style="{ animationDelay: '0.5s' }">
        <div class="stat-icon">👥</div>
        <h3>总用户数</h3>
        <p class="stat-value">{{ totalUsers }}</p>
        <div :class="['stat-trend', trends.totalUsers.direction]">
          {{ trends.totalUsers.direction === 'positive' ? '↗' : trends.totalUsers.direction === 'negative' ? '↘' : '→' }} {{ trends.totalUsers.value }}
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover" :style="{ animationDelay: '0.8s' }">
        <div class="stat-icon">📅</div>
        <h3>本周预约</h3>
        <p class="stat-value">{{ weekAppointments }}</p>
        <div :class="['stat-trend', trends.weekAppointments.direction]">
          {{ trends.weekAppointments.direction === 'positive' ? '↗' : trends.weekAppointments.direction === 'negative' ? '↘' : '→' }} {{ trends.weekAppointments.value }}
        </div>
      </el-card>
    </div>
    
    <div class="chart-row">
      <el-card class="chart-card" shadow="hover" :style="{ animationDelay: '0.9s' }">
        <template #header>
          <div class="card-header">
            <div class="header-icon">
              <i class="el-icon-data-analysis"></i>
            </div>
            <h3>近7天预约趋势</h3>
          </div>
        </template>
        <div id="trend-chart" style="width: 100%; height: 400px;"></div>
      </el-card>
      
      <el-card class="chart-card" shadow="hover" :style="{ animationDelay: '1.0s' }">
        <template #header>
          <div class="card-header">
            <div class="header-icon">
              <i class="el-icon-data-line"></i>
            </div>
            <h3>各业务类型占比</h3>
          </div>
        </template>
        <div id="business-chart" style="width: 100%; height: 400px;"></div>
      </el-card>
    </div>
    
    <el-card class="chart-card" shadow="hover" :style="{ animationDelay: '1.1s' }">
      <template #header>
        <div class="card-header">
          <div class="header-icon">
            <i class="el-icon-data-line"></i>
          </div>
          <h3>月度预约统计</h3>
        </div>
      </template>
      <div id="monthly-chart" style="width: 100%; height: 400px;"></div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { statisticsApi } from '../../api'
import { userApi } from '../../api'
import { ElMessage } from 'element-plus'

const todayTotal = ref(0)
const todayCompleted = ref(0)
const todayCanceled = ref(0)
const todayNoShow = ref(0)
const totalUsers = ref(0)
const weekAppointments = ref(0)

// 趋势数据
const trends = ref({
  todayTotal: { direction: 'positive', value: '0%' },
  todayCompleted: { direction: 'positive', value: '0%' },
  todayCanceled: { direction: 'neutral', value: '0%' },
  todayNoShow: { direction: 'neutral', value: '0%' },
  totalUsers: { direction: 'positive', value: '0%' },
  weekAppointments: { direction: 'positive', value: '0%' }
})

onMounted(() => {
  fetchTodayStatistics()
  fetchTrendData()
  fetchBusinessTypeData()
  fetchTotalCounts()
  fetchWeekAppointments()
  fetchMonthlyData()
  fetchTrends()
})

const fetchTotalCounts = async () => {
  try {
    const res = await userApi.getCount()
    totalUsers.value = res || 0
  } catch (error) {
    console.error('获取用户总数失败:', error)
    totalUsers.value = 0
  }
}

const fetchWeekAppointments = async () => {
  try {
    weekAppointments.value = await statisticsApi.getWeekStats()
  } catch (error) {
    console.error('获取本周预约失败:', error)
    weekAppointments.value = 0
  }
}

const fetchMonthlyData = async () => {
  try {
    initMonthlyChart(await statisticsApi.getMonthlyStats())
  } catch (error) {
    console.error('获取月度数据失败:', error)
    initMonthlyChart(null)
  }
}

const fetchTrends = async () => {
  try {
    const data = await statisticsApi.getTrends()
    Object.keys(data).forEach(key => {
      if (trends.value[key]) {
        trends.value[key] = data[key]
      }
    })
  } catch (error) {
    console.error('获取趋势数据失败:', error)
  }
}

const fetchTodayStatistics = async () => {
  try {
    const data = await statisticsApi.getTodayStats()
    todayTotal.value = data.total || 0
    todayCompleted.value = data.completed || 0
    todayCanceled.value = data.canceled || 0
    todayNoShow.value = data.noShow || 0
  } catch (error) {
    console.error('获取今日统计数据失败:', error)
    ElMessage.error('获取今日统计数据失败')
  }
}

const fetchTrendData = async () => {
  try {
    const data = await statisticsApi.getTrend()
    const dates = data.map(item => item.date)
    const counts = data.map(item => item.count)
    initTrendChart(dates, counts)
  } catch (error) {
    console.error('获取趋势数据失败:', error)
    ElMessage.error('获取趋势数据失败')
    initTrendChart(['3月1日', '3月2日', '3月3日', '3月4日', '3月5日', '3月6日', '3月7日'], [10, 15, 12, 18, 14, 16, 12])
  }
}

const fetchBusinessTypeData = async () => {
  try {
    initBusinessChart(await statisticsApi.getBusinessTypeStats())
  } catch (error) {
    console.error('获取业务类型数据失败:', error)
    ElMessage.error('获取业务类型数据失败')
    initBusinessChart([
      { value: 30, name: '开户' },
      { value: 25, name: '缴费' },
      { value: 20, name: '咨询' },
      { value: 25, name: '变更' }
    ])
  }
}

const initTrendChart = (dates, counts) => {
  const chart = echarts.init(document.getElementById('trend-chart'))
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['预约数量'] },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [{ data: counts, type: 'line', smooth: true, itemStyle: { color: '#667eea' } }]
  }
  chart.setOption(option)
}

const initBusinessChart = (data) => {
  const chart = echarts.init(document.getElementById('business-chart'))
  const option = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: data,
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
    }]
  }
  chart.setOption(option)
}

const initMonthlyChart = (data) => {
  const chart = echarts.init(document.getElementById('monthly-chart'))
  
  // 默认数据，当API调用失败时使用
  let defaultData = {
    months: ['1月', '2月', '3月', '4月', '5月', '6月'],
    appointmentCounts: [120, 132, 101, 134, 90, 230],
    completedCounts: [110, 120, 90, 120, 80, 210]
  }
  
  // 如果有API数据，则使用API数据
  if (data) {
    defaultData = {
      months: data.months || defaultData.months,
      appointmentCounts: data.appointmentCounts || defaultData.appointmentCounts,
      completedCounts: data.completedCounts || defaultData.completedCounts
    }
  }
  
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['预约数量', '完成数量'] },
    xAxis: {
      type: 'category',
      data: defaultData.months
    },
    yAxis: { type: 'value' },
    series: [
      {
        data: defaultData.appointmentCounts,
        type: 'bar',
        name: '预约数量',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ])
        }
      },
      {
        data: defaultData.completedCounts,
        type: 'line',
        name: '完成数量',
        itemStyle: {
          color: '#48bb78'
        }
      }
    ]
  }
  chart.setOption(option)
}
</script>

<style scoped>
.statistics {
  background-color: transparent;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 30px;
}

.statistics::before {
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

.stats-container {
  display: flex;
  gap: 25px;
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  overflow: hidden;
  padding: 20px 0;
  margin: 20px 0;
}

.stat-card {
  flex: 1;
  text-align: center;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  background: white;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.1);
  animation: fadeIn 0.8s ease-out both;
}

.stat-card::before {
  content: '';  
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transform: scaleX(0);
  transition: transform 0.4s ease;
}

.stat-card:hover {
  transform: translateY(-5px) scale(1.01);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}

.stat-card:hover::before {
  transform: scaleX(1);
}

.stat-icon {
  font-size: 3rem;
  margin-bottom: 20px;
  transition: transform 0.4s ease, color 0.4s ease;
  position: relative;
  z-index: 1;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1) rotate(5deg);
  color: #667eea;
  filter: drop-shadow(0 2px 4px rgba(102, 126, 234, 0.3));
  transform-origin: center;
}

.stat-card h3 {
  margin-bottom: 15px;
  color: #666;
  font-size: 1.1rem;
  font-weight: 600;
  transition: color 0.3s ease;
  position: relative;
  z-index: 1;
}

.stat-card:hover h3 {
  color: #667eea;
  transform: scale(1.05);
}

.stat-value {
  font-size: 2.5rem;
  font-weight: bold;
  color: #667eea;
  margin: 0;
  transition: color 0.3s ease, transform 0.3s ease;
  position: relative;
  z-index: 1;
  text-shadow: 0 2px 4px rgba(102, 126, 234, 0.2);
}

.stat-card:hover .stat-value {
  color: #5a67d8;
  transform: scale(1.1);
}

.stat-trend {
  font-size: 0.9rem;
  font-weight: 600;
  margin-top: 10px;
  padding: 5px 10px;
  border-radius: 15px;
  display: inline-block;
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
}

.stat-trend.positive {
  color: #48bb78;
  background-color: rgba(72, 187, 120, 0.1);
}

.stat-trend.negative {
  color: #f56565;
  background-color: rgba(245, 101, 101, 0.1);
}

.stat-trend.neutral {
  color: #4299e1;
  background-color: rgba(66, 153, 225, 0.1);
}

.stat-card:hover .stat-trend {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

.chart-row {
  display: flex;
  gap: 25px;
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  margin: 20px 0;
}

.chart-row .chart-card {
  flex: 1;
  margin: 0;
}

.chart-card {
  margin: 20px 0;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  background: white;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.1);
  animation: fadeIn 0.8s ease-out both;
  flex-shrink: 0;
}

.chart-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px 25px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
}

.header-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.1rem;
  transition: all 0.3s ease;
}

.header-icon:hover {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.card-header h3 {
  margin: 0;
  color: #333;
  font-size: 1.1rem;
  font-weight: 600;
  transition: color 0.3s ease;
}

.chart-card:hover .card-header h3 {
  color: #667eea;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .statistics { padding: 20px; }
  .page-title { font-size: 1.8rem; }
  .stats-container { flex-direction: column; gap: 20px; }
  .stat-card { width: 100%; max-width: 400px; margin: 0 auto; }
  .stat-value { font-size: 2rem; }
  .chart-row { flex-direction: column; gap: 20px; }
  .chart-card { margin: 10px 0; }
}

@media (max-width: 480px) {
  .stat-card { padding: 20px 15px; }
  .stat-icon { font-size: 2.5rem; }
  .card-header { padding: 15px 20px; }
}
</style>