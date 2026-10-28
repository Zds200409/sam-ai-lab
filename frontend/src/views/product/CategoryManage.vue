<template>
  <el-card>
    <div style="margin-bottom:16px;display:flex;gap:8px">
      <el-button type="primary" @click="openAdd(0)">新增顶级分类</el-button>
    </div>
    <el-tree
      :data="treeData"
      node-key="id"
      default-expand-all
      :props="{ label: 'categoryName' }"
    >
      <template #default="{ node, data }">
        <div class="tree-node">
          <span class="tree-label">
            {{ data.categoryName }}
            <el-tag size="small" type="info" style="margin-left:6px">排序:{{ data.sortOrder }}</el-tag>
          </span>
          <span class="tree-actions">
            <el-button size="small" @click.stop="openAdd(data.id)">添加子分类</el-button>
            <el-button size="small" @click.stop="openEdit(data)">编辑</el-button>
            <el-button size="small" type="danger" @click.stop="handleDelete(data.id)">删除</el-button>
          </span>
        </div>
      </template>
    </el-tree>

    <el-dialog v-model="dialogVisible" :title="form.parentId ? '新增子分类' : (form.id ? '编辑分类' : '新增顶级分类')" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.categoryName" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '../../api/product'
import { ElMessage, ElMessageBox } from 'element-plus'

const treeData = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, parentId: 0, categoryName: '', sortOrder: 0 })

onMounted(fetchData)

async function fetchData() {
  const res = await getCategoryTree()
  treeData.value = res.data
}

function openAdd(parentId) {
  form.id = null
  form.parentId = parentId
  form.categoryName = ''
  form.sortOrder = 0
  dialogVisible.value = true
}
function openEdit(row) {
  form.id = row.id
  form.parentId = row.parentId
  form.categoryName = row.categoryName
  form.sortOrder = row.sortOrder
  dialogVisible.value = true
}
async function handleSave() {
  if (form.id) {
    await updateCategory(form.id, form)
  } else {
    await createCategory(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}
async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该分类？若含子分类则无法删除', '提示', { type: 'warning' })
  await deleteCategory(id)
  ElMessage.success('已删除')
  fetchData()
}
</script>

<style scoped>
.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
}
.tree-label {
  font-size: 14px;
}
.tree-actions {
  flex-shrink: 0;
  margin-left: 16px;
}
</style>
