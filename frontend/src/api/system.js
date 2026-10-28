import request from './request'

// ==================== 用户管理 ====================
export function getUserPage(params) {
  return request({ url: '/users/page', params })
}
export function createUser(data) {
  return request({ url: '/users', method: 'post', data })
}
export function updateUser(id, data) {
  return request({ url: `/users/${id}`, method: 'put', data })
}
export function deleteUser(id) {
  return request({ url: `/users/${id}`, method: 'delete' })
}
export function resetUserPassword(id) {
  return request({ url: `/users/${id}/resetPwd`, method: 'put' })
}

// ==================== 角色管理 ====================
export function getRolePage(params) {
  return request({ url: '/roles/page', params })
}
export function createRole(data) {
  return request({ url: '/roles', method: 'post', data })
}
export function updateRole(id, data) {
  return request({ url: `/roles/${id}`, method: 'put', data })
}
export function deleteRole(id) {
  return request({ url: `/roles/${id}`, method: 'delete' })
}
export function getRoleMenus(roleId) {
  return request({ url: `/roles/${roleId}/menus` })
}
export function assignRoleMenus(roleId, menuIds) {
  return request({ url: `/roles/${roleId}/assignMenus`, method: 'put', data: menuIds })
}

// ==================== 菜单管理 ====================
export function getMenuTree() {
  return request({ url: '/menus/tree' })
}
export function createMenu(data) {
  return request({ url: '/menus', method: 'post', data })
}
export function updateMenu(id, data) {
  return request({ url: `/menus/${id}`, method: 'put', data })
}
export function deleteMenu(id) {
  return request({ url: `/menus/${id}`, method: 'delete' })
}
