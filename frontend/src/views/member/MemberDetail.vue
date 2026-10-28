<template>
  <div>
    <el-card>
      <template #header><span>会员信息</span><el-button style="float:right" @click="$router.back()">返回</el-button></template>
      <el-descriptions :column="3" border v-if="member">
        <el-descriptions-item label="会员编号">{{ member.memberNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ member.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ member.phone }}</el-descriptions-item>
        <el-descriptions-item label="当前等级">
          <el-tag :type="member.levelName === '卓越会员' ? 'warning' : 'info'">{{ member.levelName || '—' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="累计消费">{{ member.totalConsumption }}</el-descriptions-item>
        <el-descriptions-item label="积分余额">{{ member.totalPoints }}</el-descriptions-item>
        <el-descriptions-item label="累计积分">{{ member.accumulatedPoints }}</el-descriptions-item>
        <el-descriptions-item label="入会日期">{{ member.joinDate }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ member.expiryDate }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="member.status===1?'success':'warning'">{{ member.status===1?'正常':member.status===0?'冻结':'过期' }}</el-tag></el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top:16px">
      <template #header>积分变动记录</template>
      <el-table :data="pointsRecords" border stripe>
        <el-table-column prop="points" label="变动积分" width="100" />
        <el-table-column label="类型" width="100"><template #default="{row}">{{ ['','消费获得','活动获得','积分兑换','过期扣除','人工调整'][row.type] }}</template></el-table-column>
        <el-table-column prop="balanceBefore" label="变动前" width="100" />
        <el-table-column prop="balanceAfter" label="变动后" width="100" />
        <el-table-column prop="description" label="说明" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getMemberById, getMemberPointsRecords } from '../../api/member'

const route = useRoute()
const memberId = route.params.id
const member = ref(null)
const pointsRecords = ref([])

onMounted(async () => {
  const mRes = await getMemberById(memberId)
  member.value = mRes.data
  const pRes = await getMemberPointsRecords(memberId)
  pointsRecords.value = pRes.data
})
</script>
