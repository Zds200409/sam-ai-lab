<template>
  <el-card>
    <template #header>积分手动调整</template>
    <el-form :model="form" label-width="100px" style="max-width:500px">
      <el-form-item label="会员">
        <el-select v-model="form.memberId" filterable remote placeholder="搜索会员" :remote-method="searchMember" :loading="memberLoading" style="width:100%">
          <el-option v-for="m in memberOptions" :key="m.id" :label="`${m.name} - ${m.phone} (积分:${m.totalPoints})`" :value="m.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="变动积分"><el-input-number v-model="form.points" :min="-99999" /></el-form-item>
      <el-form-item label="说明"><el-input v-model="form.description" /></el-form-item>
      <el-form-item><el-button type="primary" @click="handleAdjust">确认调整</el-button></el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { getMemberPage } from '../../api/member'
import { adjustPoints } from '../../api/points'
import { ElMessage } from 'element-plus'

const form = reactive({ memberId: null, points: 0, description: '' })
const memberOptions = ref([])
const memberLoading = ref(false)

async function searchMember(query) {
  if (!query) return
  memberLoading.value = true
  try {
    const res = await getMemberPage({ phone: query, pageNum: 1, pageSize: 10 })
    memberOptions.value = res.data.records
  } finally { memberLoading.value = false }
}
async function handleAdjust() {
  if (!form.memberId) { ElMessage.warning('请选择会员'); return }
  await adjustPoints(form)
  ElMessage.success('积分调整成功')
  form.memberId = null; form.points = 0; form.description = ''
}
</script>
