<template>
  <el-card>
    <el-form :inline="true">
      <el-form-item label="按等级筛选"><el-select v-model="filterLevelId" clearable @change="fetchData"><el-option v-for="lv in levels" :key="lv.id" :label="lv.levelName" :value="lv.id" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="openAdd">新增权益</el-button></el-form-item>
    </el-form>
    <el-table :data="tableData" border stripe>
      <el-table-column prop="benefitName" label="权益名称" />
      <el-table-column prop="benefitType" label="类型" width="120" />
      <el-table-column prop="benefitValue" label="权益值" width="120" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="150">
        <template #default="{row}"><el-button size="small" @click="openEdit(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="isEdit?'编辑权益':'新增权益'" width="450px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="等级"><el-select v-model="form.levelId"><el-option v-for="lv in levels" :key="lv.id" :label="lv.levelName" :value="lv.id" /></el-select></el-form-item>
      <el-form-item label="名称"><el-input v-model="form.benefitName" /></el-form-item>
      <el-form-item label="类型"><el-select v-model="form.benefitType"><el-option label="折扣" value="DISCOUNT" /><el-option label="服务" value="SERVICE" /><el-option label="礼品" value="GIFT" /></el-select></el-form-item>
      <el-form-item label="权益值"><el-input v-model="form.benefitValue" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLevelList } from '../../api/member'
import { getBenefitList, createBenefit, updateBenefit, deleteBenefit } from '../../api/points'
import { ElMessage, ElMessageBox } from 'element-plus'

const levels = ref([])
const filterLevelId = ref(null)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ levelId: null, benefitName: '', benefitType: '', benefitValue: '', description: '' })

onMounted(async () => {
  const lvRes = await getLevelList()
  levels.value = lvRes.data
  fetchData()
})

async function fetchData() {
  const res = await getBenefitList(filterLevelId.value)
  tableData.value = res.data
}
function openAdd() { isEdit.value = false; Object.keys(form).forEach(k => form[k] = null); dialogVisible.value = true }
function openEdit(row) { isEdit.value = true; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  if (isEdit.value) {
    await updateBenefit(form.id, form)
  } else {
    await createBenefit(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}
async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await deleteBenefit(id)
  ElMessage.success('已删除')
  fetchData()
}
</script>
