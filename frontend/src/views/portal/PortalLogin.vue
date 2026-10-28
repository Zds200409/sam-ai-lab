<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">山姆会员中心</h1>
      <el-tabs v-model="activeTab" stretch @tab-change="onTabChange">
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large">
            <el-form-item prop="phone">
              <el-input v-model="loginForm.phone" placeholder="手机号" prefix-icon="Iphone" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item prop="captchaCode">
              <div class="captcha-row">
                <el-input v-model="loginForm.captchaCode" placeholder="验证码" prefix-icon="Key" @keyup.enter="handleLogin" />
                <img class="captcha-img" :src="captchaImg" alt="验证码" title="点击刷新" @click="loadCaptcha" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width:100%" :loading="loading" @click="handleLogin">登 录</el-button>
            </el-form-item>
          </el-form>
          <div class="tip">
            <el-link type="primary" :underline="false" @click="goReset">忘记密码？</el-link>
            <span class="tip-sep">|</span>
            <span>还没有账号？切换到「注册」开通会员</span>
          </div>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="手机号" prefix-icon="Iphone" />
            </el-form-item>
            <el-form-item prop="name">
              <el-input v-model="registerForm.name" placeholder="姓名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="Lock" />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" @keyup.enter="handleRegister" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width:100%" :loading="loading" @click="handleRegister">注册并登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="忘记密码" name="reset">
          <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" size="large">
            <el-form-item prop="phone">
              <el-input v-model="resetForm.phone" placeholder="手机号" prefix-icon="Iphone" />
            </el-form-item>
            <el-form-item prop="captchaCode">
              <div class="captcha-row">
                <el-input v-model="resetForm.captchaCode" placeholder="验证码" prefix-icon="Key" />
                <img class="captcha-img" :src="captchaImg" alt="验证码" title="点击刷新" @click="loadCaptcha" />
              </div>
            </el-form-item>
            <el-form-item prop="newPassword">
              <el-input v-model="resetForm.newPassword" type="password" placeholder="新密码" prefix-icon="Lock" />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="resetForm.confirmPassword" type="password" placeholder="确认新密码" prefix-icon="Lock" @keyup.enter="handleReset" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width:100%" :loading="loading" @click="handleReset">重置密码</el-button>
            </el-form-item>
          </el-form>
          <div class="tip">重置成功后请使用新密码登录</div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getCaptcha, resetPassword } from '../../api/memberPortal'
import { setMemberToken, setMemberUser } from '../../utils/memberAuth'

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)
const resetFormRef = ref(null)
const captchaImg = ref('')
const captchaKey = ref('')

const loginForm = reactive({ phone: '', password: '', captchaCode: '' })
const registerForm = reactive({ phone: '', name: '', password: '', confirmPassword: '' })
const resetForm = reactive({ phone: '', captchaCode: '', newPassword: '', confirmPassword: '' })

const loginRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const registerRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const resetRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

function afterAuth(res) {
  const user = res.data
  setMemberToken(user.token)
  setMemberUser(user)
  ElMessage.success(res.message || '登录成功')
  if (user.expired) {
    ElMessage.warning('您的会员资格已到期，请尽快续费以继续享受权益')
  }
  router.push('/portal/home')
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    captchaImg.value = res.data.captchaImage
    loginForm.captchaCode = ''
    resetForm.captchaCode = ''
  } catch {
    /* 拦截器已提示 */
  }
}

function onTabChange(name) {
  if (name === 'login' || name === 'reset') loadCaptcha()
}

function goReset() {
  activeTab.value = 'reset'
  loadCaptcha()
}

async function handleLogin() {
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await login({
      phone: loginForm.phone,
      password: loginForm.password,
      captchaKey: captchaKey.value,
      captchaCode: loginForm.captchaCode
    })
    afterAuth(res)
  } catch {
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await register({
      phone: registerForm.phone,
      name: registerForm.name,
      password: registerForm.password
    })
    afterAuth(res)
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function handleReset() {
  try {
    await resetFormRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await resetPassword({
      phone: resetForm.phone,
      captchaKey: captchaKey.value,
      captchaCode: resetForm.captchaCode,
      newPassword: resetForm.newPassword
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    activeTab.value = 'login'
    loginForm.phone = resetForm.phone
    loginForm.password = ''
    loginForm.captchaCode = ''
    resetForm.phone = ''
    resetForm.newPassword = ''
    resetForm.confirmPassword = ''
    loadCaptcha()
  } catch {
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (activeTab.value === 'login') loadCaptcha()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a2e;
}
.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}
.login-title {
  text-align: center;
  font-size: 22px;
  margin-bottom: 20px;
  color: #303133;
}
.tip {
  margin-top: 4px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}
.tip-sep {
  margin: 0 8px;
  color: #dcdfe6;
}
.captcha-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.captcha-img {
  width: 110px;
  height: 40px;
  flex-shrink: 0;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f5f7fa;
}
</style>
