import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUser } from '../utils/auth'
import { getMemberToken } from '../utils/memberAuth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/portal/login',
    name: 'PortalLogin',
    component: () => import('../views/portal/PortalLogin.vue')
  },
  {
    path: '/portal',
    name: 'PortalLayout',
    component: () => import('../views/portal/PortalLayout.vue'),
    redirect: '/portal/home',
    children: [
      { path: '/portal/home', name: 'PortalHome', component: () => import('../views/portal/PortalHome.vue'), meta: { title: '会员首页' } },
      { path: '/portal/shop', name: 'Shop', component: () => import('../views/portal/Shop.vue'), meta: { title: '商品选购' } },
      { path: '/portal/orders', name: 'MyOrders', component: () => import('../views/portal/MyOrders.vue'), meta: { title: '我的订单' } },
      { path: '/portal/returns', name: 'MyReturns', component: () => import('../views/portal/MyReturns.vue'), meta: { title: '我的退换货' } },
      { path: '/portal/points', name: 'MyPoints', component: () => import('../views/portal/MyPoints.vue'), meta: { title: '我的积分' } },
      { path: '/portal/benefits', name: 'BenefitRedeem', component: () => import('../views/portal/BenefitRedeem.vue'), meta: { title: '积分兑换' } },
      { path: '/portal/profile', name: 'MyProfile', component: () => import('../views/portal/MyProfile.vue'), meta: { title: '个人信息' } }
    ]
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: () => {
      const roles = getUser()?.roles || []
      if (roles.includes('ROLE_DELIVERY')) return '/delivery/list'
      if (roles.includes('ROLE_CASHIER')) return '/order/checkout'
      return '/home'
    },
    children: [
      { path: '/home', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
      { path: '/member/list', name: 'MemberList', component: () => import('../views/member/MemberList.vue'), meta: { title: '会员列表' } },
      { path: '/member/detail/:id', name: 'MemberDetail', component: () => import('../views/member/MemberDetail.vue'), meta: { title: '会员详情' } },
      { path: '/member/levels', name: 'LevelManage', component: () => import('../views/member/LevelManage.vue'), meta: { title: '等级管理' } },
      { path: '/product/categories', name: 'CategoryManage', component: () => import('../views/product/CategoryManage.vue'), meta: { title: '商品分类' } },
      { path: '/product/list', name: 'ProductList', component: () => import('../views/product/ProductList.vue'), meta: { title: '商品列表' } },
      { path: '/product/stock', name: 'StockManage', component: () => import('../views/product/StockManage.vue'), meta: { title: '库存管理' } },
      { path: '/order/checkout', name: 'POSCheckout', component: () => import('../views/order/POSCheckout.vue'), meta: { title: 'POS收银' } },
      { path: '/order/list', name: 'OrderList', component: () => import('../views/order/OrderList.vue'), meta: { title: '订单查询' } },
      { path: '/order/returns', name: 'ReturnList', component: () => import('../views/order/ReturnList.vue'), meta: { title: '退换货管理' } },
      { path: '/points/benefits', name: 'BenefitManage', component: () => import('../views/points/BenefitManage.vue'), meta: { title: '权益管理' } },
      { path: '/points/manage', name: 'PointsManage', component: () => import('../views/points/PointsManage.vue'), meta: { title: '积分管理' } },
      { path: '/statistics', name: 'Statistics', component: () => import('../views/statistics/StatisticsDashboard.vue'), meta: { title: '数据统计' } },
      { path: '/system/users', name: 'UserManage', component: () => import('../views/system/UserManage.vue'), meta: { title: '用户管理' } },
      { path: '/system/roles', name: 'RoleManage', component: () => import('../views/system/RoleManage.vue'), meta: { title: '角色管理' } },
      { path: '/system/menus', name: 'MenuManage', component: () => import('../views/system/MenuManage.vue'), meta: { title: '菜单管理' } },
      { path: '/system/announcements', name: 'AnnouncementManage', component: () => import('../views/system/AnnouncementManage.vue'), meta: { title: '公告管理' } },
      { path: '/system/logs', name: 'OperationLogList', component: () => import('../views/system/OperationLogList.vue'), meta: { title: '操作日志' } },
      { path: '/system/configs', name: 'ConfigManage', component: () => import('../views/system/ConfigManage.vue'), meta: { title: '参数配置' } },
      { path: '/delivery/list', name: 'DeliveryList', component: () => import('../views/delivery/DeliveryList.vue'), meta: { title: '配送订单' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = getToken()
  const memberToken = getMemberToken()

  // 会员端路由使用独立守卫
  if (to.path.startsWith('/portal')) {
    if (to.path === '/portal/login') {
      memberToken ? next('/portal/home') : next()
    } else {
      memberToken ? next() : next('/portal/login')
    }
    return
  }

  // 后台路由
  if (to.path === '/login') {
    if (token) {
      const _roles = getUser()?.roles || []
      next(_roles.includes('ROLE_DELIVERY') ? '/delivery/list'
        : _roles.includes('ROLE_CASHIER') ? '/order/checkout' : '/home')
    } else {
      next()
    }
  } else {
    if (!token) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router
