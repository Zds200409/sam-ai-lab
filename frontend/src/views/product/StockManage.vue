1<template>
  <el-card>
    <el-form :inline="true"><el-form-item label="商品名称"><el-input v-model="searchName" placeholder="搜索商品" clearable @change="searchProduct" /></el-form-item></el-form>
  </el-card>
  <el-card style="margin-top:16px">
    <el-table :data="products" border stripe v-loading="loading">
      <el-table-column prop="productCode" label="SKU" width="130" />
      <el-table-column prop="productName" label="商品名称" />
      <el-table-column prop="stockQuantity" label="当前库存" width="100" />
      <el-table-column prop="minStockAlert" label="预警阈值" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button size="small" type="primary" @click="openStockIn(row)">入库</el-button>
          <el-button size="small" @click="viewRecords(row)">流水</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top:16px;text-align:right">
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @change="fetchData" />
    </div>
  </el-card>

  <el-dialog v-model="stockDialogVisible" title="商品入库" width="400px">
    <el-form :model="stockForm">
      <el-form-item label="商品">{{ stockForm.productName }}</el-form-item>
      <el-form-item label="当前库存">{{ stockForm.currentStock }}</el-form-item>
      <el-form-item label="入库数量"><el-input-number v-model="stockForm.quantity" :min="1" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="stockForm.remark" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="stockDialogVisible=false">取消</el-button><el-button type="primary" @click="handleStockIn">确定入库</el-button></template>
  </el-dialog>

  <el-dialog v-model="recordsDialogVisible" title="库存流水" width="700px">
    <el-table :data="records" border stripe>
      <el-table-column label="类型" width="100"><template #default="{r}">{{ ['','入库','销售出库','退货入库','盘点调整'][r.type] }}</template></el-table-column>
      <el-table-column prop="quantityChange" label="变化量" width="100" />
      <el-table-column prop="stockBefore" label="变化前" width="100" />
      <el-table-column prop="stockAfter" label="变化后" width="100" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column prop="createTime" label="时间" width="170" />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getProductPage, stockIn, getStockRecords } from '../../api/product'
import { ElMessage } from 'element-plus'

const searchName = ref('')
const products = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stockDialogVisible = ref(false)
const stockForm = reactive({ productId: null, productName: '', currentStock: 0, quantity: 1, remark: '' })
const recordsDialogVisible = ref(false)
const records = ref([])

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchName.value) params.productName = searchName.value
    const res = await getProductPage(params)
    products.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
function searchProduct() { pageNum.value = 1; fetchData() }
function openStockIn(row) {
  stockForm.productId = row.id
  stockForm.productName = row.productName
  stockForm.currentStock = row.stockQuantity
  stockForm.quantity = 1
  stockForm.remark = ''
  stockDialogVisible.value = true
}
async function handleStockIn() {
  await stockIn(stockForm.productId, { quantity: stockForm.quantity, remark: stockForm.remark || '入库' })
  ElMessage.success('入库成功')
  stockDialogVisible.value = false
  fetchData()
}
async function viewRecords(row) {
  const res = await getStockRecords(row.id)
  records.value = res.data
  recordsDialogVisible.value = true
}
</script>
