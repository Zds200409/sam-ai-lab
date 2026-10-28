<template>
  <div class="pos-container">
    <!-- 左侧：会员信息 -->
    <div class="pos-left">
      <el-card>
        <template #header>会员信息</template>
        <el-select v-model="selectedMemberId" filterable remote reserve-keyword
          placeholder="输入手机号搜索会员" :remote-method="searchMember" :loading="memberLoading"
          style="width:100%" @change="onMemberSelect" clearable>
          <el-option v-for="m in memberOptions" :key="m.id" :label="`${m.name} - ${m.phone}`" :value="m.id" />
        </el-select>
        <div v-if="currentMember" class="member-info">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="姓名">{{ currentMember.name }}</el-descriptions-item>
            <el-descriptions-item label="等级">{{ currentMember.levelName }}</el-descriptions-item>
            <el-descriptions-item label="积分余额">{{ currentMember.totalPoints }}</el-descriptions-item>
            <el-descriptions-item label="累计消费">{{ currentMember.totalConsumption }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>
    </div>

    <!-- 右侧：购物车 -->
    <div class="pos-right">
      <el-card>
        <template #header>商品扫描</template>
        <el-select v-model="selectedProductId" filterable remote reserve-keyword
          placeholder="输入商品名称/条码搜索" :remote-method="searchProduct" :loading="productLoading"
          style="width:100%" @change="addToCart" clearable>
          <el-option v-for="p in productOptions" :key="p.id" :label="`${p.productName} (${p.productCode}) - ¥${p.price}`" :value="p.id">
            <div style="display:flex;align-items:center;gap:8px">
              <el-image v-if="p.imageUrl" :src="p.imageUrl" fit="cover" style="width:32px;height:32px;border-radius:4px;flex-shrink:0" />
              <span>{{ p.productName }} ({{ p.productCode }}) - ¥{{ p.price }}</span>
            </div>
          </el-option>
        </el-select>
      </el-card>

      <el-card style="margin-top:16px">
        <template #header>购物车</template>
        <el-table :data="cart" border stripe>
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="price" label="单价" width="100" />
          <el-table-column label="数量" width="140">
            <template #default="{row, $index}">
              <el-input-number v-model="row.quantity" :min="1" :max="99" size="small"  />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="100">
            <template #default="{row}">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="" width="60">
            <template #default="{ $index }"><el-button size="small" type="danger" @click="cart.splice($index,1);">删除</el-button></template>
          </el-table-column>
        </el-table>

        <div class="cart-summary">
          <div class="summary-row">商品总数: <b>{{ cart.reduce((s,i)=>s+i.quantity,0) }}</b></div>
          <div class="summary-row">商品总额: <b>¥{{ totalAmount.toFixed(2) }}</b></div>
          <div class="summary-row">等级折扣: <b>-¥{{ discountAmount.toFixed(2) }}</b></div>
          <div class="summary-row">实付金额: <b style="color:#f56c6c;font-size:24px">¥{{ actualAmount.toFixed(2) }}</b></div>
          <div class="summary-row">预计积分: <b>{{ estimatedPoints }}</b></div>
        </div>

        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:12px">
          <el-button @click="cart=[];">清空购物车</el-button>
          <el-select v-model="paymentMethod" style="width:140px">
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="现金" value="CASH" />
            <el-option label="银行卡" value="BANK_CARD" />
          </el-select>
          <el-button type="success" size="large" :disabled="!canCheckout" :loading="checkingOut" @click="handleCheckout">
            结算 (F8)
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 结算成功弹窗 -->
    <el-dialog v-model="receiptVisible" title="结算成功" width="500px">
      <div class="receipt">
        <h3 style="text-align:center">山姆会员商店</h3>
        <p>订单号: {{ receipt.orderNo }}</p>
        <p>会员: {{ receipt.memberName }}</p>
        <hr />
        <div v-for="(item,i) in receipt.items" :key="i">
          {{ item.productName }} x{{ item.quantity }}  ¥{{ item.subtotal }}
        </div>
        <hr />
        <p>总额: ¥{{ receipt.totalAmount }} | 折扣: ¥{{ receipt.discountAmount }}</p>
        <p style="font-size:18px;font-weight:bold">实付: ¥{{ receipt.actualAmount }}</p>
        <p>获得积分: {{ receipt.pointsEarned }}</p>
        <p style="text-align:center;color:#909399">{{ receipt.createTime }}</p>
      </div>
      <template #footer>
        <el-button @click="receiptVisible=false;resetCheckout()">继续收银</el-button>
        <el-button type="primary" @click="printReceipt">打印小票</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMemberPage, getMemberById } from '../../api/member'
import { getLevelList } from '../../api/member'
import { searchProducts } from '../../api/product'
import { checkout } from '../../api/order'

const selectedMemberId = ref(null)
const memberOptions = ref([])
const memberLoading = ref(false)
const currentMember = ref(null)

const selectedProductId = ref(null)
const productOptions = ref([])
const productLoading = ref(false)

const cart = ref([])
const paymentMethod = ref('WECHAT')
const checkingOut = ref(false)
const receiptVisible = ref(false)
const receipt = ref({})

const totalAmount = computed(() => cart.value.reduce((s, i) => s + i.price * i.quantity, 0))
const discountRate = ref(1)
const discountAmount = computed(() => (totalAmount.value * (1 - discountRate.value)))
const actualAmount = computed(() => totalAmount.value - discountAmount.value)
const estimatedPoints = computed(() => {
  const rate = currentMember.value?.pointsRate || 1
  return Math.floor(actualAmount.value * rate)
})
const canCheckout = computed(() => currentMember.value && cart.value.length > 0)

onMounted(() => { window.addEventListener('keydown', onKeydown) })
onUnmounted(() => { window.removeEventListener('keydown', onKeydown) })

// 用 watch 监听选择变化（比 @change 更可靠）
watch(selectedMemberId, (newId) => { if (newId) onMemberSelect(newId) })
watch(selectedProductId, (newId) => { if (newId) addToCart(newId) })

function onKeydown(e) { if (e.key === 'F8') { e.preventDefault(); if (canCheckout.value) handleCheckout() } }

async function searchMember(query) {
  if (!query) return
  memberLoading.value = true
  try {
    const res = await getMemberPage({ phone: query, pageNum: 1, pageSize: 10 })
    memberOptions.value = res.data.records
  } finally { memberLoading.value = false }
}

async function onMemberSelect(id) {
  if (!id) return
  try {
    const res = await getMemberById(id)
    const m = res.data
    if (!m) { ElMessage.warning('会员信息加载失败'); return }
    const lvRes = await getLevelList()
    const level = lvRes.data.find(l => l.id === m.currentLevelId)
    currentMember.value = {
      id: m.id,
      name: m.name,
      phone: m.phone,
      totalPoints: m.totalPoints,
      totalConsumption: m.totalConsumption,
      levelName: level?.levelName || '',
      discountRate: level?.discountRate || 1,
      pointsRate: level?.pointsRate || 1
    }
    discountRate.value = currentMember.value.discountRate
  } catch (e) {
    ElMessage.error('加载会员信息失败')
  }
}

async function searchProduct(query) {
  if (!query) return
  productLoading.value = true
  try {
    const res = await searchProducts(query)
    productOptions.value = res.data
  } finally { productLoading.value = false }
}

function addToCart(productId) {
  const product = productOptions.value.find(p => p.id === productId)
  if (!product) return
  const exist = cart.value.find(i => i.productId === productId)
  if (exist) {
    exist.quantity++
  } else {
    cart.value.push({ productId: product.id, productName: product.productName, productCode: product.productCode, price: product.price, quantity: 1 })
  }
  selectedProductId.value = null
}

async function handleCheckout() {
  if (!currentMember.value) { ElMessage.warning('请先选择会员'); return }
  if (cart.value.length === 0) { ElMessage.warning('购物车为空'); return }
  checkingOut.value = true
  try {
    const res = await checkout({
      memberId: currentMember.value.id,
      items: cart.value.map(i => ({ productId: i.productId, quantity: i.quantity })),
      paymentMethod: paymentMethod.value
    })
    receipt.value = res.data
    receiptVisible.value = true
    ElMessage.success('结算成功')
  } catch (e) {
    // 拦截器已处理网络/业务错误，此处兜底非请求异常
    if (e && e.message && !e.message.includes('请求')) {
      ElMessage.error(e.message || '结算失败，请重试')
    }
  } finally { checkingOut.value = false }
}

function resetCheckout() {
  cart.value = []
  selectedMemberId.value = null
  currentMember.value = null
  discountRate.value = 1
}

function printReceipt() {
  const w = window.open('', '', 'width=400,height=600')
  w.document.write(`<html><body style="font-family:monospace;font-size:12px">${document.querySelector('.receipt').innerHTML}</body></html>`)
  w.print()
}
</script>

<style scoped>
.pos-container { display: flex; gap: 16px; }
.pos-left { width: 320px; flex-shrink: 0; }
.pos-right { flex: 1; }
.member-info { margin-top: 12px; }
.cart-summary { margin-top: 16px; text-align: right; }
.summary-row { margin-bottom: 4px; font-size: 14px; }
.receipt { font-size: 14px; line-height: 1.8; }
.receipt p { margin: 4px 0; }
</style>
