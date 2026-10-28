<template>
  <div>
    <el-alert class="balance-tip" type="success" :closable="false" show-icon>
      当前可用积分：<b>{{ myPoints }}</b>
    </el-alert>

    <el-row :gutter="16">
      <el-col :span="8" v-for="b in list" :key="b.id">
        <el-card class="benefit-card" v-loading="loading">
          <div class="benefit-head">
            <span class="benefit-name">{{ b.benefitName }}</span>
            <el-tag size="small" :type="typeTag(b.benefitType)">{{ typeText(b.benefitType) }}</el-tag>
          </div>
          <div class="benefit-desc">{{ b.description }}</div>
          <div class="benefit-foot">
            <span v-if="b.pointsCost > 0" class="cost">{{ b.pointsCost }} 积分</span>
            <span v-else class="auto">自动享受</span>
            <el-button
              v-if="b.pointsCost > 0"
              type="primary"
              size="small"
              :disabled="myPoints < b.pointsCost"
              @click="handleRedeem(b)"
            >兑换</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && list.length === 0" description="暂无可用权益" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBenefits, getSummary, redeem } from '../../api/memberPortal'

const loading = ref(false)
const list = ref([])
const myPoints = ref(0)

const typeMap = { DISCOUNT: '折扣', SERVICE: '服务', GIFT: '礼品' }
const typeTagMap = { DISCOUNT: 'success', SERVICE: 'info', GIFT: 'warning' }

function typeText(t) {
  return typeMap[t] || t
}
function typeTag(t) {
  return typeTagMap[t] || 'info'
}

async function load() {
  loading.value = true
  try {
    const [benefitsRes, summaryRes] = await Promise.all([getBenefits(), getSummary()])
    list.value = benefitsRes.data || []
    myPoints.value = summaryRes.data.totalPoints || 0
  } finally {
    loading.value = false
  }
}

async function handleRedeem(b) {
  try {
    await ElMessageBox.confirm(`确认使用 ${b.pointsCost} 积分兑换「${b.benefitName}」？`, '积分兑换', { type: 'warning' })
  } catch {
    return
  }
  try {
    await redeem(b.id)
    ElMessage.success('兑换成功')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

onMounted(load)
</script>

<style scoped>
.balance-tip {
  margin-bottom: 16px;
}
.benefit-card {
  margin-bottom: 16px;
}
.benefit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.benefit-name {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
}
.benefit-desc {
  font-size: 13px;
  color: #909399;
  margin: 10px 0;
  min-height: 20px;
}
.benefit-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cost {
  font-size: 15px;
  font-weight: bold;
  color: #e6a23c;
}
.auto {
  font-size: 13px;
  color: #67c23a;
}
</style>
