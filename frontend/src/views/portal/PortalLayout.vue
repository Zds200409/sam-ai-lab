<template>
  <el-container class="portal-layout">
    <el-aside width="220px" class="aside">
      <div class="logo">山姆会员中心</div>
      <el-menu :default-active="$route.path" router background-color="#1a1a2e" text-color="#cfd3dc" active-text-color="#ffd04b">
        <el-menu-item index="/portal/home">
          <el-icon><HomeFilled /></el-icon><span>会员首页</span>
        </el-menu-item>
        <el-menu-item index="/portal/shop">
          <el-icon><ShoppingCart /></el-icon><span>商品选购</span>
        </el-menu-item>
        <el-menu-item index="/portal/orders">
          <el-icon><List /></el-icon><span>我的订单</span>
        </el-menu-item>
        <el-menu-item index="/portal/returns">
          <el-icon><RefreshLeft /></el-icon><span>我的退换货</span>
        </el-menu-item>
        <el-menu-item index="/portal/points">
          <el-icon><Star /></el-icon><span>我的积分</span>
        </el-menu-item>
        <el-menu-item index="/portal/benefits">
          <el-icon><Present /></el-icon><span>积分兑换</span>
        </el-menu-item>
        <el-menu-item index="/portal/profile">
          <el-icon><User /></el-icon><span>个人信息</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title || '' }}</span>
        <div class="user-box">
          <span class="user-name">{{ user?.name || user?.memberNo || '会员' }}</span>
          <el-button link type="primary" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { getMemberUser, clearMemberAuth } from '../../utils/memberAuth'

const router = useRouter()
const user = computed(() => getMemberUser())

async function handleLogout() {
  await ElMessageBox.confirm('确定退出会员中心吗？', '提示', { type: 'warning' })
  clearMemberAuth()
  router.push('/portal/login')
}
</script>

<style scoped>
.portal-layout {
  min-height: 100vh;
}
.aside {
  background: #1a1a2e;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 1px;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}
.page-title {
  font-size: 16px;
  color: #303133;
}
.user-box {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name {
  color: #303133;
}
.main {
  background: #f5f7fa;
}
</style>
