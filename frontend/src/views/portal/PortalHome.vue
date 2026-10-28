<template>
  <div>
    <el-card class="member-card" v-loading="loading">
      <div class="card-head">
        <div class="avatar">{{ (summary.name || '会').charAt(0) }}</div>
        <div class="info">
          <div class="name">{{ summary.name }} <el-tag size="small" type="warning">{{ summary.levelName }}</el-tag></div>
          <div class="no">会员卡号：{{ summary.memberNo }}</div>
          <div class="no">到期日期：{{ summary.expiryDate || '—' }}</div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="stats">
      <el-col :span="8">
        <el-card>
          <div class="stat-label">积分余额</div>
          <div class="stat-value">{{ summary.totalPoints ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-label">累计积分</div>
          <div class="stat-value">{{ summary.accumulatedPoints ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-label">累计消费（元）</div>
          <div class="stat-value">{{ summary.totalConsumption ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="quick">
      <div class="quick-title">快捷入口</div>
      <el-space wrap>
        <el-button type="primary" plain @click="$router.push('/portal/orders')">我的订单</el-button>
        <el-button type="warning" plain @click="$router.push('/portal/points')">我的积分</el-button>
        <el-button type="success" plain @click="$router.push('/portal/benefits')">积分兑换</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSummary } from '../../api/memberPortal'

const loading = ref(false)
const summary = ref({})

async function load() {
  loading.value = true
  try {
    const res = await getSummary()
    summary.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.member-card {
  margin-bottom: 16px;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 20px;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #ffd04b;
  color: #fff;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}
.no {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}
.stats {
  margin-bottom: 16px;
}
.stat-label {
  font-size: 13px;
  color: #909399;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-top: 8px;
}
.quick-title {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
}
</style>
