<template>
  <el-card>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ row.type === 2 ? '换货' : '退货' }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="申请原因" min-width="180" show-overflow-tooltip />
      <el-table-column label="涉及金额" width="110">
        <template #default="{ row }">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handleTime" label="处理时间" width="170">
        <template #default="{ row }">{{ row.handleTime || '—' }}</template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="handlePage"
    />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyReturns } from '../../api/memberPortal'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const statusMap = { 1: '待审核', 2: '已通过', 3: '已拒绝' }
const statusTagMap = { 1: 'warning', 2: 'success', 3: 'danger' }

function statusText(s) {
  return statusMap[s] || s
}
function statusTag(s) {
  return statusTagMap[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getMyReturns({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handlePage(p) {
  pageNum.value = p
  load()
}

onMounted(load)
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
