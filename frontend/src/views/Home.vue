<template>
  <div>
    <!-- 统计卡片骨架 -->
    <el-skeleton :loading="loading" animated :throttle="200">
      <template #template>
        <el-row :gutter="20">
          <el-col :span="6"><el-skeleton-item variant="p" style="height:88px" /></el-col>
          <el-col :span="6"><el-skeleton-item variant="p" style="height:88px" /></el-col>
          <el-col :span="6"><el-skeleton-item variant="p" style="height:88px" /></el-col>
          <el-col :span="6"><el-skeleton-item variant="p" style="height:88px" /></el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top:12px">
          <el-col :span="6"><el-skeleton-item variant="p" style="height:88px" /></el-col>
          <el-col :span="6"><el-skeleton-item variant="p" style="height:88px" /></el-col>
        </el-row>
      </template>
      <template #default>
        <el-row :gutter="20">
          <el-col :span="6" v-for="card in cards" :key="card.label">
            <el-card class="stat-card" shadow="hover">
              <div class="card-inner">
                <div>
                  <div class="card-label">{{ card.label }}</div>
                  <div class="card-value">{{ card.value }}</div>
                </div>
                <el-icon :size="40" :color="card.color"><component :is="card.icon" /></el-icon>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top:12px">
          <el-col :span="6" v-for="card in reminderCards" :key="card.label">
            <el-card class="stat-card" shadow="hover">
              <div class="card-inner">
                <div>
                  <div class="card-label">{{ card.label }}</div>
                  <div class="card-value">{{ card.value }}</div>
                </div>
                <el-icon :size="40" :color="card.color"><component :is="card.icon" /></el-icon>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-skeleton>

    <!-- 图表骨架 -->
    <el-skeleton :loading="chartsLoading" animated :throttle="200">
      <template #template>
        <el-row :gutter="20" style="margin-top:20px">
          <el-col :span="12"><el-skeleton-item variant="p" style="height:320px" /></el-col>
          <el-col :span="12"><el-skeleton-item variant="p" style="height:320px" /></el-col>
        </el-row>
      </template>
      <template #default>
        <el-row :gutter="20" style="margin-top:20px">
          <el-col :span="12">
            <el-card><div ref="trendChart" style="height:320px"></div></el-card>
          </el-col>
          <el-col :span="12">
            <el-card><div ref="categoryChart" style="height:320px"></div></el-card>
          </el-col>
        </el-row>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboard, getSalesTrend, getSalesByCategory } from '../api/statistics'

const loading = ref(true)
const chartsLoading = ref(true)

const cards = ref([
  { label: '今日订单数', value: 0, icon: 'Sell', color: '#409EFF' },
  { label: '今日销售额', value: '0.00', icon: 'Money', color: '#67C23A' },
  { label: '本月销售额', value: '0.00', icon: 'TrendCharts', color: '#E6A23C' },
  { label: '会员总数', value: 0, icon: 'UserFilled', color: '#F56C6C' }
])
const reminderCards = ref([
  { label: '今日生日会员', value: 0, icon: 'Present', color: '#F56C6C' },
  { label: '7天内到期', value: 0, icon: 'Clock', color: '#E6A23C' }
])

const trendChart = ref(null)
const categoryChart = ref(null)
const chartInstances = []

function handleResize() {
  chartInstances.forEach(c => c && c.resize())
}

onMounted(async () => {
  try {
    const dashRes = await getDashboard()
    const d = dashRes.data
    cards.value[0].value = d.todayOrderCount
    cards.value[1].value = d.todayAmount
    cards.value[2].value = d.monthAmount
    cards.value[3].value = d.totalMembers
    reminderCards.value[0].value = (d.todayBirthdays && d.todayBirthdays.length) || 0
    reminderCards.value[1].value = (d.expiringSoon && d.expiringSoon.length) || 0
  } catch (e) {
    console.error('Home dashboard error:', e)
  }
  loading.value = false

  await nextTick()

  try {
    const trendRes = await getSalesTrend(30)
    const tDates = trendRes.data.map(i => i.date)
    const tAmounts = trendRes.data.map(i => i.totalAmount)
    const tChart = echarts.init(trendChart.value)
    chartInstances.push(tChart)
    tChart.setOption({
      title: { text: '近30天销售趋势', left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: { data: tDates, axisLabel: { rotate: 45 } },
      yAxis: {},
      series: [{ name: '销售额', type: 'line', data: tAmounts, smooth: true, areaStyle: {} }]
    })
  } catch (e) {
    console.error('Trend chart error:', e)
  }

  try {
    const catRes = await getSalesByCategory()
    const cChart = echarts.init(categoryChart.value)
    chartInstances.push(cChart)
    cChart.setOption({
      title: { text: '分类销售占比', left: 'center' },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: catRes.data.map(i => ({ name: i.name, value: i.value }))
      }]
    })
  } catch (e) {
    console.error('Category chart error:', e)
  }
  chartsLoading.value = false

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstances.forEach(c => c && c.dispose())
})
</script>

<style scoped>
.stat-card { cursor: pointer; }
.card-inner { display: flex; justify-content: space-between; align-items: center; }
.card-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.card-value { font-size: 28px; font-weight: bold; color: #303133; }
</style>
