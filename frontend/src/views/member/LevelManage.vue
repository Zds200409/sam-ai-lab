<template>
  <div>
    <el-card>
      <div style="margin-bottom:16px"><el-button type="primary" @click="openAdd">新增等级</el-button></div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="levelName" label="等级名称" width="150" />
        <el-table-column prop="pointsRate" label="积分倍率" width="120" />
        <el-table-column prop="discountRate" label="折扣率" width="120" />
        <el-table-column prop="annualFee" label="年费(元)" width="120" />
        <el-table-column label="操作">
          <template #default="{row}"><el-button size="small" @click="openEdit(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑等级':'新增等级'" width="450px">
      <el-form ref="formRef" :model="form" label-width="120px">
        <el-form-item label="等级名称"><el-input v-model="form.levelName" /></el-form-item>
        <el-form-item label="积分倍率"><el-input-number v-model="form.pointsRate" :min="0" :precision="2" :step="0.1" /></el-form-item>
        <el-form-item label="折扣率"><el-input-number v-model="form.discountRate" :min="0" :max="1" :precision="2" :step="0.01" /></el-form-item>
        <el-form-item label="年费(元)"><el-input-number v-model="form.annualFee" :min="0" :precision="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLevelPage, createLevel, updateLevel } from '../../api/member'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ levelName: '', pointsRate: 1, discountRate: 1, annualFee: 0, sortOrder: 0 })

onMounted(fetchData)
async function fetchData() {
  const res = await getLevelPage({ pageNum: 1, pageSize: 50 })
  tableData.value = res.data.records
}

function openAdd() {
  isEdit.value = false
  Object.keys(form).forEach(k => form[k] = k === 'pointsRate' ? 1 : k === 'discountRate' ? 1 : 0)
  dialogVisible.value = true
}
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}
async function handleSave() {
  if (isEdit.value) {
    await updateLevel(form.id, form)
  } else {
    await createLevel(form)
  }
  ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
  dialogVisible.value = false
  fetchData()
}
</script>
