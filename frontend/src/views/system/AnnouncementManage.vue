<template>
  <el-card>
    <div style="margin-bottom:16px;display:flex;gap:8px">
      <el-button type="primary" @click="openAdd">发布公告</el-button>
      <el-button @click="fetchData">刷新</el-button>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publisher" label="发布人" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="success" :disabled="row.status === 1" @click="handlePublish(row)">发布</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNum"
      @current-change="p => { query.pageNum = p; fetchData() }"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:160px">
            <el-option label="活动通知" :value="1" />
            <el-option label="权益公告" :value="2" />
            <el-option label="续费提醒" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布人"><el-input v-model="form.publisher" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
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
import {
  getAnnouncementPage, createAnnouncement, updateAnnouncement,
  publishAnnouncement, deleteAnnouncement
} from '../../api/announcement'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10 })
const form = reactive({ id: null, title: '', content: '', type: 1, publisher: '系统管理员', status: 0 })

onMounted(fetchData)

function typeText(t) {
  return t === 1 ? '活动通知' : t === 2 ? '权益公告' : '续费提醒'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAnnouncementPage(query)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function openAdd() {
  Object.assign(form, { id: null, title: '', content: '', type: 1, publisher: '系统管理员', status: 0 })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.title) {
    ElMessage.warning('请填写标题')
    return
  }
  if (form.id) {
    await updateAnnouncement(form.id, form)
  } else {
    await createAnnouncement(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}

async function handlePublish(row) {
  await ElMessageBox.confirm(`确认发布公告「${row.title}」？`, '提示', { type: 'warning' })
  await publishAnnouncement(row.id)
  ElMessage.success('已发布')
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除公告「${row.title}」？`, '提示', { type: 'warning' })
  await deleteAnnouncement(row.id)
  ElMessage.success('已删除')
  fetchData()
}
</script>
