<template>
  <div>
    <!-- 日期筛选 -->
    <el-card style="margin-bottom:16px">
      <el-form :inline="true">
        <el-form-item label="销售趋势日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
            @change="onDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="resetDate">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20" v-if="dashboard">
      <el-col :span="6"><el-card class="stat-card"><div class="label">今日订单</div><div class="value">{{ dashboard.todayOrderCount }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="label">今日销售额</div><div class="value">¥{{ dashboard.todayAmount }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="label">今日新会员</div><div class="value">{{ dashboard.todayNewMembers }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="label">待续费会员</div><div class="value">{{ dashboard.pendingRenewals }}</div></el-card></el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12"><el-card><div ref="trendChart" style="height:350px"></div></el-card></el-col>
      <el-col :span="12"><el-card><div ref="catChart" style="height:350px"></div></el-card></el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12"><el-card><div ref="levelChart" style="height:350px"></div></el-card></el-col>
      <el-col :span="12"><el-card><div ref="topChart" style="height:350px"></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboard, getSalesTrend, getSalesByCategory, getMemberAnalysis, getTopProducts } from '../../api/statistics'

const dashboard = ref(null)
const trendChart = ref(null)
const catChart = ref(null)
const levelChart = ref(null)
const topChart = ref(null)
const dateRange = ref(null)

const dateShortcuts = [
  { text: '最近7天', value: () => { const e = new Date(); const s = new Date(); s.setDate(s.getDate() - 7); return [s, e] } },
  { text: '最近30天', value: () => { const e = new Date(); const s = new Date(); s.setDate(s.getDate() - 30); return [s, e] } },
  { text: '本月', value: () => { const e = new Date(); const s = new Date(); s.setDate(1); return [s, e] } }
]

// 存储所有 ECharts 实例，用于 resize 和销毁
const chartInstances = []

let trendChartInstance = null
let trendTitle = '近30天销售趋势'

function handleResize() {
  chartInstances.forEach(c => c && c.resize())
}

onMounted(async () => {
  try {
    const dashRes = await getDashboard()
    dashboard.value = dashRes.data
  } catch (e) {
    // 错误已在拦截器处理
  }

  await nextTick()
  try {
    initCharts()
  } catch (e) {
    // 图表初始化失败，静默降级
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstances.forEach(c => c && c.dispose())
})

async function initCharts() {
  await refreshTrendChart()

  const catRes = await getSalesByCategory()
  initPieChart(catChart.value, '分类销售占比', catRes.data)

  const memberRes = await getMemberAnalysis()
  initPieChart(levelChart.value, '会员等级分布', memberRes.data.levelDistribution.map(i => ({ name: i.name, value: i.value })))

  const topRes = await getTopProducts(10)
  initBarChart(topChart.value, '热销商品TOP10', topRes.data.map(i => i.name), topRes.data.map(i => i.quantity))
}

async function refreshTrendChart() {
  const [start, end] = dateRange.value || []
  const trendRes = await getSalesTrend(30, start, end)
  const title = start && end ? `${start} ~ ${end} 销售趋势` : '近30天销售趋势'
  if (trendChartInstance) {
    chartInstances.splice(chartInstances.indexOf(trendChartInstance), 1)
    trendChartInstance.dispose()
  }
  trendChartInstance = initLineChart(trendChart.value, title, trendRes.data.map(i => i.date), trendRes.data.map(i => i.totalAmount))
}

async function onDateChange() {
  await refreshTrendChart()
}

function resetDate() {
  dateRange.value = null
  onDateChange()
}

function initLineChart(el, title, xData, yData) {
  const c = echarts.init(el)
  chartInstances.push(c)
  c.setOption({
    title: { text: title, left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { data: xData, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: {},
    series: [{ type: 'line', data: yData, smooth: true, areaStyle: {} }]
  })
  return c
}
function initPieChart(el, title, data) {
  const c = echarts.init(el)
  chartInstances.push(c)
  c.setOption({ title: { text: title, left: 'center' }, tooltip: { trigger: 'item' }, series: [{ type: 'pie', radius: ['40%', '70%'], data }] })
}
function initBarChart(el, title, xData, yData) {
  const c = echarts.init(el)
  chartInstances.push(c)
  c.setOption({
    title: { text: title, left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { data: xData, axisLabel: { rotate: 30, fontSize: 10 } },
    yAxis: {},
    series: [{ type: 'bar', data: yData }]
  })
}
</script>

<style scoped>
.stat-card { text-align: center; cursor: pointer; }
.label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.value { font-size: 28px; font-weight: bold; color: #303133; }
</style>
