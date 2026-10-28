<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">山姆会员消费管理系统</h1>
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="验证码" prefix-icon="Key" @keyup.enter="handleLogin" />
            <img class="captcha-img" :src="captchaImg" alt="验证码" title="点击刷新" @click="loadCaptcha" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { getCaptcha } from '../api/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const captchaImg = ref('')
const form = reactive({ username: 'admin', password: '123456', captchaKey: '', captchaCode: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    form.captchaKey = res.data.captchaKey
    captchaImg.value = res.data.captchaImage
    form.captchaCode = ''
  } catch (e) {
    ElMessage.error('验证码加载失败')
  }
}

async function handleLogin() {
  try {
    await formRef.value.validate()
  } catch {
    // Element Plus 表单会自动显示校验错误信息
    return
  }
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    // 根据角色菜单跳转到第一个可用页面
    const menus = userStore.menus || []
    // 配送员角色不展示首页（首页依赖全局经营统计，配送员无权限），登录后直达配送列表
    const loginRoles = userStore.userInfo?.roles || []
    if (loginRoles.includes('ROLE_DELIVERY')) {
      router.push('/delivery/list')
      return
    }
    if (loginRoles.includes('ROLE_CASHIER')) {
      router.push('/order/checkout')
      return
    }
    let firstPage = '/home'
    // 有首页菜单就优先进首页
    const hasHome = menus.some(m => m.path === '/home')
    if (!hasHome) {
      for (const m of menus) {
        if (m.children && m.children.length) {
          firstPage = m.children[0].path
          break
        }
        if (m.path && m.menuType === 'C') {
          firstPage = m.path
          break
        }
      }
    }
    router.push(firstPage)
  } catch (e) {
    // 登录失败（含验证码错误）时刷新验证码，避免旧验证码已被消费
    loadCaptcha()
    // 拦截器已处理网络/业务错误，此处兜底非请求异常
    if (e && e.message && !e.message.includes('请求')) {
      ElMessage.error(e.message)
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
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
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
}
.login-title {
  text-align: center;
  font-size: 22px;
  margin-bottom: 30px;
  color: #303133;
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
