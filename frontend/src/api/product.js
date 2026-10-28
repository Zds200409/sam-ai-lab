import request from './request'

// ==================== 商品分类 ====================
export function getCategoryTree() {
  return request({ url: '/categories/tree' })
}
export function createCategory(data) {
  return request({ url: '/categories', method: 'post', data })
}
export function updateCategory(id, data) {
  return request({ url: `/categories/${id}`, method: 'put', data })
}
export function deleteCategory(id) {
  return request({ url: `/categories/${id}`, method: 'delete' })
}

// ==================== 商品 CRUD ====================
export function getProductPage(params) {
  return request({ url: '/products/page', params })
}
export function getProductById(id) {
  return request({ url: `/products/${id}` })
}
export function searchProducts(keyword) {
  return request({ url: '/products/search', params: { keyword } })
}
export function createProduct(data) {
  return request({ url: '/products', method: 'post', data })
}
export function updateProduct(id, data) {
  return request({ url: `/products/${id}`, method: 'put', data })
}
export function toggleProductStatus(id) {
  return request({ url: `/products/${id}/status`, method: 'put' })
}

// ==================== 库存 ====================
export function stockIn(productId, data) {
  return request({ url: `/products/${productId}/stock/in`, method: 'post', data })
}
export function getStockRecords(productId) {
  return request({ url: `/products/${productId}/stock-records` })
}
