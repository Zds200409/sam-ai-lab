<template>
  <div>
    <el-card>
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="姓名"><el-input v-model="searchForm.name" placeholder="会员姓名" clearable /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="searchForm.phone" placeholder="手机号" clearable /></el-form-item>
        <el-form-item label="等级"><el-select v-model="searchForm.levelId" placeholder="全部" clearable><el-option v-for="lv in levels" :key="lv.id" :label="lv.levelName" :value="lv.id" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="searchForm.status" placeholder="全部" clearable><el-option label="正常" :value="1" /><el-option label="冻结" :value="0" /><el-option label="过期" :value="2" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="search">搜索</el-button><el-button @click="resetSearch">重置</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:16px">
      <div style="margin-bottom:16px">
        <el-button type="primary" @click="openAdd">新增会员</el-button>
      </div>
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="memberNo" label="会员编号" width="150" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="性别" width="70"><template #default="{row}">{{ ['未知','男','女'][row.gender] }}</template></el-table-column>
        <el-table-column prop="totalConsumption" label="累计消费" width="120" />
        <el-table-column prop="totalPoints" label="积分余额" width="100" />
        <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':row.status===0?'warning':'info'">{{ row.status===1?'正常':row.status===0?'冻结':'过期' }}</el-tag></template></el-table-column>
        <el-table-column prop="expiryDate" label="到期日" width="120" />
        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="$router.push(`/member/detail/${row.id}`)">详情</el-button>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status===1" size="small" type="warning" @click="freeze(row)">冻结</el-button>
            <el-button v-if="row.status===0" size="small" type="success" @click="unfreeze(row)">解冻</el-button>
            <el-button size="small" type="primary" @click="renew(row)">续费</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑会员':'新增会员'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="性别"><el-radio-group v-model="form.gender"><el-radio :value="0">未知</el-radio><el-radio :value="1">男</el-radio><el-radio :value="2">女</el-radio></el-radio-group></el-form-item>
        <el-form-item label="生日"><el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="等级"><el-select v-model="form.currentLevelId"><el-option v-for="lv in levels" :key="lv.id" :label="lv.levelName" :value="lv.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMemberPage, createMember, updateMember, freezeMember, unfreezeMember, renewMember } from '../../api/member'
import { getLevelList } from '../../api/member'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchForm = reactive({ name: '', phone: '', levelId: null, status: null })
const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const levels = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ name: '', phone: '', gender: 0, birthday: '', email: '', currentLevelId: null })
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

onMounted(async () => {
  try {
    const lvRes = await getLevelList()
    levels.value = lvRes.data
  } catch (e) {
    // 降级：等级列表加载失败不影响主流程
  }
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getMemberPage({ pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function search() { pageNum.value = 1; fetchData() }
function resetSearch() { searchForm.name = ''; searchForm.phone = ''; searchForm.levelId = null; searchForm.status = null; search() }

function openAdd() {
  isEdit.value = false
  Object.keys(form).forEach(k => form[k] = k === 'gender' ? 0 : k === 'currentLevelId' ? null : '')
  dialogVisible.value = true
}
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, { id: row.id, name: row.name, phone: row.phone, gender: row.gender, birthday: row.birthday, email: row.email, currentLevelId: row.currentLevelId })
  dialogVisible.value = true
}
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateMember(form.id, form)
  } else {
    await createMember(form)
  }
  ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
  dialogVisible.value = false
  fetchData()
}

async function freeze(row) {
  await ElMessageBox.confirm('确定冻结该会员？', '提示')
  await freezeMember(row.id)
  ElMessage.success('已冻结')
  fetchData()
}
async function unfreeze(row) {
  await unfreezeMember(row.id)
  ElMessage.success('已解冻')
  fetchData()
}
async function renew(row) {
  await ElMessageBox.confirm('确定续费一年？', '提示')
  await renewMember(row.id)
  ElMessage.success('续费成功')
  fetchData()
}
</script>
