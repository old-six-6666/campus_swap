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
}
