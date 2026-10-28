<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" clearable placeholder="全部" @change="search">
          <el-option label="待配送" :value="1" />
          <el-option label="配送中" :value="2" />
          <el-option label="已送达" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
    </el-form>
  </el-card>
  <el-card style="margin-top:16px">
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="memberName" label="会员" width="100" />
      <el-table-column prop="deliveryAddress" label="地址" />
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="['','warning','primary','success'][row.status]">
            {{ ['','待配送','配送中','已送达'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button v-if="row.status===1" size="small" type="primary" @click="handlePickup(row)">接单</el-button>
          <el-button v-if="row.status===2" size="small" type="success" @click="handleComplete(row)">送达</el-button>
          <el-tag v-if="row.status===3" type="success" size="small">已完成</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top:16px;text-align:right">
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        layout="total, sizes, prev, pager, next" @change="fetchData" />
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDeliveryPage, pickupDelivery, completeDelivery } from '../../api/delivery'

const searchForm = reactive({ status: null })
const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

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
    const res = await getDeliveryPage({ pageNum: pageNum.value, pageSize: pageSize.value, status: searchForm.status })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
function search() { pageNum.value = 1; fetchData() }
async function handlePickup(row) {
  await pickupDelivery(row.id)
  ElMessage.success('已接单，开始配送')
  fetchData()
}
async function handleComplete(row) {
  await completeDelivery(row.id)
  ElMessage.success('配送完成')
  fetchData()
}
</script>
