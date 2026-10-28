<template>
  <el-card>
    <div style="margin-bottom:16px"><el-button type="primary" @click="openAdd(0)">新增顶级菜单</el-button></div>
    <el-table :data="menuTree" border stripe row-key="id" default-expand-all>
      <el-table-column prop="menuName" label="菜单名称" />
      <el-table-column prop="menuType" label="类型" width="80"><template #default="{row}">{{ {M:'目录',C:'菜单',F:'按钮'}[row.menuType] }}</template></el-table-column>
      <el-table-column prop="path" label="路由" width="180" />
      <el-table-column prop="perms" label="权限标识" width="180" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{row}">
          <el-button size="small" @click="openAdd(row.id)">添加子级</el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.parentId?'新增子菜单':'新增/编辑菜单'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.menuType"><el-option label="目录" value="M" /><el-option label="菜单" value="C" /><el-option label="按钮" value="F" /></el-select></el-form-item>
        <el-form-item label="路由" v-if="form.menuType!=='F'"><el-input v-model="form.path" /></el-form-item>
        <el-form-item label="组件" v-if="form.menuType==='C'"><el-input v-model="form.component" /></el-form-item>
        <el-form-item label="权限标识" v-if="form.menuType==='F'"><el-input v-model="form.perms" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '../../api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const menuTree = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, parentId: 0, menuName: '', menuType: 'M', path: '', component: '', perms: '', icon: '', sortOrder: 0 })

onMounted(fetchData)

async function fetchData() {
  const res = await getMenuTree()
  menuTree.value = res.data
}
function openAdd(parentId) {
  form.id = null; form.parentId = parentId; form.menuName = ''; form.menuType = 'M'; form.path = ''; form.component = ''; form.perms = ''; form.icon = ''; form.sortOrder = 0
  dialogVisible.value = true
}
function openEdit(row) { Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  if (form.id) {
    await updateMenu(form.id, form)
  } else {
    await createMenu(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}
async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除？子菜单会一并删除', '提示', { type: 'warning' })
  await deleteMenu(id)
  ElMessage.success('已删除')
  fetchData()
}
</script>
