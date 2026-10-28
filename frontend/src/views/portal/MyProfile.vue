<template>
  <el-alert v-if="profile.expired" type="warning" :closable="false" show-icon style="margin-bottom:16px"
    title="您的会员资格已到期"
    description="年费已过期，请到门店或通过会员升级续费，以继续享受会员权益。" />

  <el-row :gutter="16">
    <el-col :span="12">
      <el-card header="个人资料">
        <el-form :model="form" label-width="80px" v-loading="loading">
          <el-form-item label="会员卡号">
            <el-input :model-value="profile.memberNo" disabled />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input :model-value="profile.phone" disabled />
          </el-form-item>
          <el-form-item label="等级">
            <el-input :model-value="profile.levelName" disabled />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择生日" style="width:100%" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存资料</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card header="修改密码">
        <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="changing" @click="handleChangePwd">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>

  <el-card style="margin-top:16px" header="会员升级">
    <div v-if="upgradeInfo && upgradeInfo.upgradable">
      <p>当前等级：<el-tag type="info">{{ upgradeInfo.currentLevelName }}</el-tag>
        可升级为 <el-tag type="warning">{{ upgradeInfo.nextLevelName }}</el-tag></p>
      <p>需补差价：<span style="color:#f56c6c;font-size:18px;font-weight:bold">¥ {{ upgradeInfo.diffAmount }}</span></p>
      <el-button type="primary" :loading="upgrading" @click="handleUpgrade">补差价升级为{{ upgradeInfo.nextLevelName }}</el-button>
    </div>
    <el-alert v-else-if="upgradeInfo" type="success" :closable="false"
      :title="`您当前已是${upgradeInfo.currentLevelName || '最高'}等级，无需升级`" />
    <span v-else>加载中…</span>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, changePassword, getUpgradeInfo, upgradeLevel } from '../../api/memberPortal'

const loading = ref(false)
const saving = ref(false)
const changing = ref(false)
const upgrading = ref(false)
const profile = ref({})
const upgradeInfo = ref(null)
const form = reactive({ name: '', gender: 1, birthday: '', email: '' })

const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function load() {
  loading.value = true
  try {
    const res = await getProfile()
    profile.value = res.data
    form.name = res.data.name
    form.gender = res.data.gender || 1
    form.birthday = res.data.birthday || ''
    form.email = res.data.email || ''
    const infoRes = await getUpgradeInfo()
    upgradeInfo.value = infoRes.data
  } finally {
    loading.value = false
  }
}

async function handleUpgrade() {
  try {
    await ElMessage.confirm(
      `确认支付补差价 ¥${upgradeInfo.value.diffAmount} 升级为 ${upgradeInfo.value.nextLevelName} 吗？`,
      '会员升级'
    )
  } catch {
    return
  }
  upgrading.value = true
  try {
    const res = await upgradeLevel({ targetLevelId: upgradeInfo.value.nextLevelId, paidAmount: upgradeInfo.value.diffAmount, paymentMethod: '到店支付' })
    ElMessage.success(`升级成功，已补差价 ¥${res.data.diffAmount}，支付单号 ${res.data.paymentNo}`)
    load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    upgrading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateProfile({ ...form })
    ElMessage.success('资料已更新')
    load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function handleChangePwd() {
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  changing.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch {
    /* 拦截器已提示 */
  } finally {
    changing.value = false
  }
}

onMounted(load)
</script>
