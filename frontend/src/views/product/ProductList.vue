<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="商品名称"><el-input v-model="searchForm.productName" clearable /></el-form-item>
      <el-form-item label="分类"><el-select v-model="searchForm.categoryId" clearable placeholder="全部"><el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" /></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="searchForm.status" clearable placeholder="全部"><el-option label="上架" :value="1" /><el-option label="下架" :value="0" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
    </el-form>
  </el-card>
  <el-card style="margin-top:16px">
    <div style="margin-bottom:16px"><el-button type="primary" @click="openAdd">新增商品</el-button></div>
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column label="图片" width="80">
        <template #default="{row}">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="cover" style="width:50px;height:50px;border-radius:4px" :preview-src-list="[row.imageUrl]" preview-teleported />
          <span v-else style="color:#c0c4cc;font-size:12px">无图</span>
        </template>
      </el-table-column>
      <el-table-column prop="productCode" label="SKU" width="130" />
      <el-table-column prop="productName" label="商品名称" />
      <el-table-column prop="price" label="售价" width="100" />
      <el-table-column prop="stockQuantity" label="库存" width="80" />
      <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'">{{ row.status===1?'上架':'下架' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.status===1?'warning':'success'" @click="toggleStatus(row)">{{ row.status===1?'下架':'上架' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top:16px;text-align:right">
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="fetchData" />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="isEdit?'编辑商品':'新增商品'" width="550px">
    <el-form ref="formRef" :model="form" label-width="100px">
      <el-form-item label="商品名称"><el-input v-model="form.productName" /></el-form-item>
      <el-form-item label="SKU编码"><el-input v-model="form.productCode" /></el-form-item>
      <el-form-item label="条形码"><el-input v-model="form.barcode" /></el-form-item>
      <el-form-item label="所属分类"><el-select v-model="form.categoryId"><el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" /></el-select></el-form-item>
      <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
      <el-form-item label="售价"><el-input-number v-model="form.price" :precision="2" :min="0" /></el-form-item>
      <el-form-item label="成本价"><el-input-number v-model="form.costPrice" :precision="2" :min="0" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getProductPage, createProduct, updateProduct, toggleProductStatus } from '../../api/product'
import { getCategoryTree } from '../../api/product'
import { ElMessage } from 'element-plus'

const searchForm = reactive({ productName: '', categoryId: null, status: null })
const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const categories = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ productName: '', productCode: '', barcode: '', categoryId: null, unit: '', price: 0, costPrice: 0, description: '' })

onMounted(async () => {
  const catRes = await getCategoryTree()
  categories.value = flattenTree(catRes.data)
  fetchData()
})

function flattenTree(tree) { const r = []; function w(n) { n.forEach(x => { r.push(x); if(x.children) w(x.children) }) }; w(tree); return r }

async function fetchData() {
  loading.value = true
  try {
    const res = await getProductPage({ pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
function search() { pageNum.value = 1; fetchData() }
function openAdd() { isEdit.value = false; Object.keys(form).forEach(k => form[k] = k === 'price' || k === 'costPrice' ? 0 : null); dialogVisible.value = true }
function openEdit(row) { isEdit.value = true; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  if (isEdit.value) {
    await updateProduct(form.id, form)
  } else {
    await createProduct(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}
async function toggleStatus(row) {
  await toggleProductStatus(row.id)
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  fetchData()
}
</script>
