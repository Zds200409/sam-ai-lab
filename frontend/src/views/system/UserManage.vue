<template>
  <div>
    <el-card>
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名"><el-input v-model="searchForm.username" clearable /></el-form-item>
        <el-form-item label="状态"><el-select v-model="searchForm.status" clearable><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card style="margin-top:16px">
      <div style="margin-bottom:16px"><el-button type="primary" @click="openAdd">新增用户</el-button></div>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'danger'">{{ row.status===1?'启用':'禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{row}">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑用户':'新增用户'" width="450px">
      <el-form ref="formRef" :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="isEdit" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { getUserPage, createUser, updateUser, deleteUser, resetUserPassword } from '../../api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchForm = reactive({ username: '', status: null })
const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', realName: '', phone: '', email: '' })

fetchData()

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserPage({ pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
function search() { pageNum.value = 1; fetchData() }

function openAdd() { isEdit.value = false; Object.keys(form).forEach(k => form[k] = ''); dialogVisible.value = true }
function openEdit(row) { isEdit.value = true; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  if (isEdit.value) {
    await updateUser(form.id, { id: form.id, username: form.username, realName: form.realName, phone: form.phone, email: form.email })
  } else {
    await createUser(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}

async function handleResetPwd(row) {
  await ElMessageBox.confirm(`确定重置用户 ${row.username} 的密码为 123456？`, '提示')
  await resetUserPassword(row.id)
  ElMessage.success('密码已重置')
}

async function handleDelete(row) {
  if (row.username === 'admin') { ElMessage.warning('不能删除超级管理员'); return }
  await ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  fetchData()
}
</script>
