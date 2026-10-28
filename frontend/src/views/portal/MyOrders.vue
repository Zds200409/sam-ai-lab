<template>
  <el-card>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="createTime" label="下单时间" width="180" />
      <el-table-column prop="totalAmount" label="商品金额" width="110" />
      <el-table-column prop="discountAmount" label="优惠金额" width="110" />
      <el-table-column prop="actualAmount" label="实付金额" width="110">
        <template #default="{ row }">¥{{ row.actualAmount }}</template>
      </el-table-column>
      <el-table-column prop="pointsEarned" label="获得积分" width="100" />
      <el-table-column prop="paymentMethod" label="支付方式" width="110" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" :disabled="row.status !== 1" @click="openReturn(row)">
            申请退换货
          </el-button>
        </template>
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

    <el-dialog v-model="dialogVisible" title="申请退换货" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="订单号">
          <el-input v-model="form.orderNo" disabled />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">退货</el-radio>
            <el-radio :value="2">换货</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请说明退换货原因，例如商品破损、规格不符等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOrders, applyReturn } from '../../api/memberPortal'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const form = reactive({ orderId: null, orderNo: '', type: 1, reason: '' })

const statusMap = { 1: '已完成', 2: '待审核退款', 3: '已退款', 4: '退款已拒绝', 5: '已作废' }
const statusTagMap = { 1: 'success', 2: 'warning', 3: 'info', 4: 'danger', 5: 'info' }

function statusText(s) {
  return statusMap[s] || s
}
function statusTag(s) {
  return statusTagMap[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getOrders({ pageNum: pageNum.value, pageSize: pageSize.value })
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

function openReturn(row) {
  form.orderId = row.id
  form.orderNo = row.orderNo
  form.type = 1
  form.reason = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.reason.trim()) {
    ElMessage.warning('请填写退换货原因')
    return
  }
  submitting.value = true
  try {
    await applyReturn({ orderId: form.orderId, type: form.type, reason: form.reason })
    ElMessage.success('退换货申请已提交，可在“我的退换货”中查看进度')
    dialogVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
