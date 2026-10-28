<template>
  <el-card>
    <div style="margin-bottom:16px;display:flex;gap:8px;align-items:center">
      <el-input v-model="keyword" placeholder="参数键 / 名称" clearable style="width:200px" @keyup.enter="fetchData" />
      <el-button type="primary" @click="fetchData">查询</el-button>
      <el-button @click="() => { keyword = ''; fetchData() }">重置</el-button>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="configKey" label="参数键" width="220" />
      <el-table-column prop="configName" label="参数名称" width="160" />
      <el-table-column prop="configValue" label="参数值" min-width="220" show-overflow-tooltip />
      <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="编辑参数" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="参数键"><el-input v-model="form.configKey" disabled /></el-form-item>
        <el-form-item label="参数名称"><el-input v-model="form.configName" /></el-form-item>
        <el-form-item label="参数值"><el-input v-model="form.configValue" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getConfigList, updateConfig } from '../../api/config'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')
const dialogVisible = ref(false)
const form = reactive({ id: null, configKey: '', configName: '', configValue: '', remark: '' })

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    const res = await getConfigList({ keyword: keyword.value })
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  await updateConfig(form.id, {
    configValue: form.configValue,
    configName: form.configName,
    remark: form.remark
  })
  ElMessage.success('参数已保存')
  dialogVisible.value = false
  fetchData()
}
</script>
