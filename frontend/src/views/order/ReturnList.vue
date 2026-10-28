<template>
  <el-card>
    <div style="margin-bottom:16px;display:flex;gap:8px;align-items:center">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width:140px" @change="fetchData">
        <el-option label="待审核" :value="1" />
        <el-option label="已通过" :value="2" />
        <el-option label="已拒绝" :value="3" />
      </el-select>
      <el-button type="primary" @click="openAdd">登记退换货</el-button>
      <el-button @click="fetchData">刷新</el-button>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="orderNo" label="订单编号" width="200" />
      <el-table-column prop="memberName" label="会员姓名" width="110" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'danger' : 'warning'" size="small">
            {{ row.type === 1 ? '退货' : '换货' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 1">
            <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
            <el-button size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
          </template>
          <span v-else style="color:#909399">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNum"
      @current-change="handlePageChange"
    />

    <el-dialog v-model="dialogVisible" title="登记退换货" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="订单ID"><el-input v-model.number="form.orderId" placeholder="请输入订单ID" /></el-form-item>
        <el-form-item label="订单编号"><el-input v-model="form.orderNo" /></el-form-item>
        <el-form-item label="会员姓名"><el-input v-model="form.memberName" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">退货</el-radio>
            <el-radio :value="2">换货</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额"><el-input-number v-model="form.amount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="form.reason" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getReturnPage, createReturn, approveReturn, rejectReturn } from '../../api/return'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, status: null })
const form = reactive({ orderId: null, orderNo: '', memberName: '', type: 1, amount: 0, reason: '' })

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    const res = await getReturnPage(query)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  query.pageNum = page
  fetchData()
}

function statusText(s) {
  return s === 1 ? '待审核' : s === 2 ? '已通过' : '已拒绝'
}
function statusType(s) {
  return s === 1 ? 'warning' : s === 2 ? 'success' : 'info'
}

function openAdd() {
  Object.assign(form, { orderId: null, orderNo: '', memberName: '', type: 1, amount: 0, reason: '' })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.orderId) {
    ElMessage.warning('请填写订单ID')
    return
  }
  await createReturn(form)
  ElMessage.success('退换货申请已提交')
  dialogVisible.value = false
  fetchData()
}

async function handleApprove(row) {
  await ElMessageBox.confirm(`确认通过订单 ${row.orderNo} 的退换货申请？`, '提示', { type: 'warning' })
  await approveReturn(row.id)
  ElMessage.success('已通过')
  fetchData()
}

async function handleReject(row) {
  await ElMessageBox.confirm(`确认拒绝订单 ${row.orderNo} 的退换货申请？`, '提示', { type: 'warning' })
  await rejectReturn(row.id)
  ElMessage.success('已拒绝')
  fetchData()
}
</script>
