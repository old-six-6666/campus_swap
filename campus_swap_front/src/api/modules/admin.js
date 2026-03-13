import request from '@/api/index'

export const adminApi = {
  // ===== 用户管理 =====
  /** 分页查询用户列表 */
  listUsers: (params) => request.get('/admin/users', { params }),
  /** 禁用 / 启用用户 */
  updateUserStatus: (id, status) => request.put(`/admin/users/${id}/status`, null, { params: { status } }),
  /** 修改用户角色（超管） */
  updateUserRole: (id, role) => request.put(`/admin/users/${id}/role`, null, { params: { role } }),
  /** 删除用户（超管） */
  deleteUser: (id) => request.delete(`/admin/users/${id}`),

  // ===== 商品管理 =====
  /** 分页查询所有商品 */
  listItems: (params) => request.get('/admin/items', { params }),
  /** 修改商品状态 */
  updateItemStatus: (id, status) => request.put(`/admin/items/${id}/status`, null, { params: { status } }),
  /** 强制删除商品 */
  deleteItem: (id) => request.delete(`/admin/items/${id}`),

  // ===== 管理员权限管理（超管专用） =====
  /** 分页查询所有管理员（role=1），含权限 */
  listAdmins: (params) => request.get('/admin/admins', { params }),
  /** 全量覆盖某管理员的权限 */
  setAdminPermissions: (id, permissions) => request.put(`/admin/admins/${id}/permissions`, { permissions }),

  // ===== 商品审核（ITEM_AUDIT） =====
  /** 分页查询待审核商品 */
  listPendingItems: (params) => request.get('/admin/items/pending', { params }),
  /** 审核商品：action=1 通过  action=2 拒绝 */
  auditItem: (id, action, remark) => request.put(`/admin/items/${id}/audit`, { action, remark }),

  // ===== 当前用户权限 =====
  /** 获取当前登录管理员的权限列表 */
  getMyPermissions: () => request.get('/admin/me/permissions'),
}
