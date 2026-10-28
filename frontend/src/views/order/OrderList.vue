<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="订单号"><el-input v-model="searchForm.orderNo" clearable /></el-form-item>
      <el-form-item label="会员"><el-input v-model="searchForm.memberName" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" clearable placeholder="全部">
          <el-option label="已完成" :value="1" />
          <el-option label="待审核退款" :value="2" />
          <el-option label="已退款" :value="3" />
          <el-option label="退款已拒绝" :value="4" />
          <el-option label="已作废" :value="5" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
    </el-form>
  </el-card>
  <el-card style="margin-top:16px">
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="memberName" label="会员" width="100" />
      <el-table-column prop="totalAmount" label="总额" width="100" />
      <el-table-column prop="discountAmount" label="折扣" width="100" />
      <el-table-column prop="actualAmount" label="实付" width="100" />
      <el-table-column prop="pointsEarned" label="积分" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="showDetail(row)">详情</el-button>
          <el-button v-if="row.status===1" size="small" type="warning" @click="handleRefund(row)">申请退款</el-button>
          <el-button v-if="row.status===2" size="small" type="success" @click="handleApprove(row)">通过</el-button>
          <el-button v-if="row.status===2" size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top:16px;text-align:right">
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="fetchData" />
    </div>
  </el-card>

  <el-dialog v-model="detailVisible" title="订单详情" width="550px">
    <el-descriptions :column="2" border v-if="detail">
      <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="会员">{{ detail.memberName }}</el-descriptions-item>
      <el-descriptions-item label="总额">¥{{ detail.totalAmount }}</el-descriptions-item>
      <el-descriptions-item label="折扣">¥{{ detail.discountAmount }}</el-descriptions-item>
      <el-descriptions-item label="实付">¥{{ detail.actualAmount }}</el-descriptions-item>
      <el-descriptions-item label="积分">{{ detail.pointsEarned }}</el-descriptions-item>
    </el-descriptions>
    <el-table :data="detail?.items" border style="margin-top:12px">
      <el-table-column prop="productName" label="商品" />
      <el-table-column prop="unitPrice" label="单价" width="100" />
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="subtotal" label="小计" width="100" />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderPage, getOrderById, refundOrder, approveRefund, rejectRefund } from '../../api/order'

const searchForm = reactive({ orderNo: '', memberName: '', status: null })
const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detail = ref(null)
const detailVisible = ref(false)

const STATUS_MAP = { 1: '已完成', 2: '待审核退款', 3: '已退款', 4: '退款已拒绝', 5: '已作废' }
const STATUS_TYPE = { 1: 'success', 2: 'warning', 3: 'info', 4: 'danger', 5: 'info' }

function statusText(code) { return STATUS_MAP[code] || '未知' }
function statusType(code) { return STATUS_TYPE[code] || 'info' }

fetchData()

// ---- 自动刷新：会员端下单后，后台无需手动刷新即可看到新数据 ----
const REFRESH_MS = 15000
let refreshTimer = null
function silentRefresh() {
  if (document.hidden || loading.value) return
  fetchData(true)
}
function onWindowFocus() { silentRefresh() }
function onVisibilityChange() { if (!document.hidden) silentRefresh() }
onMounted(() => {
  refreshTimer = setInterval(silentRefresh, REFRESH_MS)
  window.addEventListener('focus', onWindowFocus)
  document.addEventListener('visibilitychange', onVisibilityChange)
})
onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('focus', onWindowFocus)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  try {
    const res = await getOrderPage({ pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
function search() { pageNum.value = 1; fetchData() }

async function showDetail(row) {
  try {
    const res = await getOrderById(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) { /* 错误已在拦截器处理 */ }
}

async function handleRefund(row) {
  try {
    await ElMessageBox.confirm('确定申请退款？将提交管理员审核', '提示', { type: 'warning' })
    await refundOrder(row.id)
    ElMessage.success('退款申请已提交')
    fetchData()
  } catch (e) { if (e !== 'cancel') { /* 错误已在拦截器处理 */ } }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm('确定审核通过？将恢复库存、退回积分和金额', '提示', { type: 'warning' })
    await approveRefund(row.id)
    ElMessage.success('退款已审核通过')
    fetchData()
  } catch (e) { if (e !== 'cancel') { /* 错误已在拦截器处理 */ } }
}

async function handleReject(row) {
  try {
    await ElMessageBox.confirm('确定拒绝退款申请？', '提示', { type: 'warning' })
    await rejectRefund(row.id)
    ElMessage.success('退款申请已拒绝')
    fetchData()
  } catch (e) { if (e !== 'cancel') { /* 错误已在拦截器处理 */ } }
}
</script>
