<template>
  <div class="shop">
    <el-row :gutter="16">
      <el-col :span="6" v-for="p in products" :key="p.id">
        <el-card class="product-card" :body-style="{ padding: '0' }" shadow="hover">
          <el-image :src="p.imageUrl" fit="cover" class="product-img" />
          <div class="product-body">
            <div class="product-name" :title="p.productName">{{ p.productName }}</div>
            <div class="product-meta">库存 {{ p.stockQuantity }} {{ p.unit }}</div>
            <div class="product-foot">
              <span class="product-price">¥{{ p.price }}</span>
              <el-button
                type="primary"
                size="small"
                :disabled="p.stockQuantity <= 0"
                @click="addToCart(p)"
              >加入购物车</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && products.length === 0" description="暂无上架商品" />

    <!-- 底部购物车悬浮栏 -->
    <div class="cart-bar" v-if="cart.length > 0">
      <div class="cart-info">
        已选 <b>{{ cartCount }}</b> 件，合计 <b class="cart-total">¥{{ cartTotal.toFixed(2) }}</b>
      </div>
      <el-button type="success" size="large" :loading="submitting" @click="cartVisible = true">去结算</el-button>
    </div>

    <!-- 购物清单 -->
    <el-drawer v-model="cartVisible" title="购物清单" size="400px">
      <div v-if="cart.length === 0" class="empty-cart">购物车为空</div>
      <template v-else>
        <div v-for="item in cart" :key="item.productId" class="cart-item">
          <el-image :src="item.imageUrl" fit="cover" class="cart-img" />
          <div class="cart-item-info">
            <div class="cart-item-name">{{ item.productName }}</div>
            <div class="cart-item-price">¥{{ item.price }}</div>
          </div>
          <el-input-number v-model="item.quantity" :min="1" :max="item.stock" size="small" @change="onQtyChange" />
          <el-button link type="danger" @click="removeItem(item.productId)">删除</el-button>
        </div>
        <div class="cart-footer">
          <div class="address-block">
            <div class="address-label">收货地址（线上配送订单必填）</div>
            <el-input
              v-model="deliveryAddress"
              type="textarea"
              :rows="2"
              maxlength="100"
              show-word-limit
              placeholder="请填写收货人、联系电话及详细地址，如：张三 13800000000 山姆会员店附近小区16栋"
            />
          </div>
          <div class="cart-total-line">合计 ¥{{ cartTotal.toFixed(2) }}</div>
          <el-button type="success" size="large" style="width:100%" :loading="submitting" @click="submitOrder">提交订单（在线支付）</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 下单成功 -->
    <el-dialog v-model="successVisible" title="下单成功" width="440px">
      <div class="success-body">
        <p>订单号：{{ orderResult.orderNo }}</p>
        <p>实付金额：¥{{ orderResult.actualAmount }}</p>
        <p>获得积分：{{ orderResult.pointsEarned }}</p>
        <p class="hint">请到门店收银台完成支付</p>
      </div>
      <template #footer>
        <el-button @click="successVisible = false">继续选购</el-button>
        <el-button type="primary" @click="goOrders">查看我的订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProducts, placeOrder } from '../../api/memberPortal'

const router = useRouter()
const loading = ref(false)
const products = ref([])
const cart = ref([])
const cartVisible = ref(false)
const submitting = ref(false)
const successVisible = ref(false)
const orderResult = ref({})
const deliveryAddress = ref('')

const cartCount = computed(() => cart.value.reduce((s, i) => s + i.quantity, 0))
const cartTotal = computed(() => cart.value.reduce((s, i) => s + i.price * i.quantity, 0))

async function load() {
  loading.value = true
  try {
    const res = await getProducts({ pageNum: 1, pageSize: 100 })
    products.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

function addToCart(p) {
  const exist = cart.value.find(i => i.productId === p.id)
  if (exist) {
    exist.quantity++
  } else {
    cart.value.push({ productId: p.id, productName: p.productName, imageUrl: p.imageUrl, price: p.price, stock: p.stockQuantity, quantity: 1 })
  }
  ElMessage.success('已加入购物车')
}

function removeItem(productId) {
  cart.value = cart.value.filter(i => i.productId !== productId)
}

function onQtyChange() {
  // 数量变化，合计自动重算
}

async function submitOrder() {
  if (cart.value.length === 0) return
  if (!deliveryAddress.value || !deliveryAddress.value.trim()) {
    ElMessage.warning('线上配送订单需填写收货地址')
    return
  }
  submitting.value = true
  try {
    const res = await placeOrder({
      items: cart.value.map(i => ({ productId: i.productId, quantity: i.quantity })),
      paymentMethod: '在线支付',
      deliveryAddress: deliveryAddress.value.trim()
    })
    orderResult.value = res.data
    cart.value = []
    cartVisible.value = false
    successVisible.value = true
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

function goOrders() {
  successVisible.value = false
  router.push('/portal/orders')
}

onMounted(load)
</script>

<style scoped>
.shop {
  padding-bottom: 80px;
}
.product-card {
  margin-bottom: 16px;
}
.product-img {
  width: 100%;
  height: 160px;
  display: block;
}
.product-body {
  padding: 10px 12px;
}
.product-name {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-meta {
  font-size: 12px;
  color: #909399;
  margin: 6px 0;
}
.product-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.product-price {
  font-size: 18px;
  color: #e6a23c;
  font-weight: bold;
}
.cart-bar {
  position: fixed;
  left: 220px;
  right: 0;
  bottom: 0;
  height: 60px;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 100;
}
.cart-info {
  font-size: 15px;
  color: #303133;
}
.cart-total {
  color: #f56c6c;
  font-size: 20px;
}
.empty-cart {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.cart-img {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  flex-shrink: 0;
}
.cart-item-info {
  flex: 1;
}
.cart-item-name {
  font-size: 14px;
  color: #303133;
}
.cart-item-price {
  font-size: 13px;
  color: #e6a23c;
  margin-top: 4px;
}
.cart-footer {
  margin-top: 20px;
}
.address-block {
  margin-bottom: 12px;
}
.address-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
  font-weight: 600;
}
.cart-total-line {
  text-align: right;
  font-size: 16px;
  color: #303133;
  margin-bottom: 12px;
}
.success-body {
  line-height: 2;
  font-size: 14px;
}
.hint {
  color: #e6a23c;
}
</style>
