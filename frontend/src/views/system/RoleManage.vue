<template>
  <div>
    <el-card>
      <div style="margin-bottom:16px"><el-button type="primary" @click="openAdd">新增角色</el-button></div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="200">
          <template #default="{row}">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="openAssignMenus(row)">分配菜单</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑角色':'新增角色'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.roleCode" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="500px" @opened="onMenuDialogOpened">
      <el-tree ref="treeRef" :data="menuTree" show-checkbox node-key="id" default-expand-all />
      <template #footer><el-button @click="menuDialogVisible=false">取消</el-button><el-button type="primary" @click="handleAssignMenus">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getRolePage, createRole, updateRole, deleteRole, assignRoleMenus, getMenuTree, getRoleMenus } from '../../api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ roleName: '', roleCode: '', description: '' })
const menuDialogVisible = ref(false)
const menuTree = ref([])
const treeRef = ref(null)
const currentRoleId = ref(null)
const pendingMenuIds = ref([])

onMounted(fetchData)

async function fetchData() {
  const res = await getRolePage({ pageNum: 1, pageSize: 50 })
  tableData.value = res.data.records
}
function openAdd() { isEdit.value = false; Object.keys(form).forEach(k => form[k] = ''); dialogVisible.value = true }
function openEdit(row) { isEdit.value = true; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  if (isEdit.value) {
    await updateRole(form.id, form)
  } else {
    await createRole(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}
async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await deleteRole(id)
  ElMessage.success('已删除')
  fetchData()
}
async function openAssignMenus(row) {
  currentRoleId.value = row.id
  const res = await getMenuTree()
  menuTree.value = res.data
  // 先取该角色已分配的菜单，弹窗打开后再回显勾选
  const rm = await getRoleMenus(row.id)
  pendingMenuIds.value = rm.data || []
  menuDialogVisible.value = true
}
function onMenuDialogOpened() {
  // 弹窗完全打开后再设置勾选，避免树尚未渲染
  treeRef.value.setCheckedKeys(pendingMenuIds.value)
}
async function handleAssignMenus() {
  const checkedKeys = treeRef.value.getCheckedKeys()
  const halfKeys = treeRef.value.getHalfCheckedKeys()
  const menuIds = [...checkedKeys, ...halfKeys]
  if (menuIds.length === 0) {
    await ElMessageBox.confirm('未勾选任何菜单，保存后该角色的菜单权限将被清空，是否继续？', '提示', { type: 'warning' })
  }
  await assignRoleMenus(currentRoleId.value, menuIds)
  ElMessage.success('菜单权限已分配')
  menuDialogVisible.value = false
}
</script>
