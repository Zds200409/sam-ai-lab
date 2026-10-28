<template>
  <el-card>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="row.type === 3 ? 'danger' : 'success'">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="积分变动" width="120">
        <template #default="{ row }">
          <span :style="{ color: row.points > 0 ? '#67c23a' : '#f56c6c' }">{{ row.points > 0 ? '+' + row.points : row.points }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="balanceBefore" label="变动前" width="100" />
      <el-table-column prop="balanceAfter" label="变动后" width="100" />
      <el-table-column prop="description" label="说明" min-width="200" />
    </el-table>
    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="handlePage"
    />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPointsRecords } from '../../api/memberPortal'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const typeMap = { 1: '消费获得', 2: '活动获得', 3: '积分兑换', 4: '过期扣除', 5: '人工调整' }

function typeText(t) {
  return typeMap[t] || t
}

async function load() {
  loading.value = true
  try {
    const res = await getPointsRecords({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handlePage(p) {
  pageNum.value = p
  load()
}

onMounted(load)
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
