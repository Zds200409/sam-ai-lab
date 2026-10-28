<template>
  <el-card>
    <div style="margin-bottom:16px;display:flex;gap:8px;align-items:center">
      <el-input v-model="query.username" placeholder="操作人" clearable style="width:150px" @keyup.enter="handleSearch" />
      <el-input v-model="query.operation" placeholder="操作内容" clearable style="width:170px" @keyup.enter="handleSearch" />
      <el-select v-model="query.status" placeholder="全部结果" clearable style="width:130px" @change="handleSearch">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="username" label="操作人" width="120" />
      <el-table-column prop="operation" label="操作内容" width="160" />
      <el-table-column prop="method" label="调用方法" min-width="220" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column prop="duration" label="耗时(ms)" width="100" />
      <el-table-column label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="170" />
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNum"
      @current-change="p => { query.pageNum = p; fetchData() }"
    />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOperationLogPage } from '../../api/log'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, username: '', operation: '', status: null })

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    const res = await getOperationLogPage(query)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchData()
}

function handleReset() {
  Object.assign(query, { pageNum: 1, pageSize: 10, username: '', operation: '', status: null })
  fetchData()
}
</script>
