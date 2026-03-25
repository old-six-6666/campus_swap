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

  // ===== 学生档案管理（STUDENT_MANAGE） =====
  /** 分页查询学生档案 */
  listStudentRecords: (params) => request.get('/admin/students', { params }),
  /** 添加单条学生档案 */
  addStudentRecord: (data) => request.post('/admin/students', data),
  /** 批量导入学生档案 */
  batchImportStudentRecords: (data) => request.post('/admin/students/batch', data),
  /** 删除学生档案 */
  deleteStudentRecord: (id) => request.delete(`/admin/students/${id}`),

  // ===== 学生认证审核（STUDENT_MANAGE） =====
  /** 分页查询认证申请列表 */
  listVerifications: (params) => request.get('/admin/verifications', { params }),
  /** 审核认证申请：action=1 通过  action=2 拒绝 */
  reviewVerification: (id, action, remark) => request.put(`/admin/verifications/${id}/review`, { action, remark }),

  // ===== 举报内容审核（CONTENT_AUDIT） =====
  /** 分页查询举报列表 */
  listReports: (params) => request.get('/admin/reports', { params }),
  /** 审核举报：action=1 处理(内容下架)  action=2 驳回(内容正常) */
  reviewReport: (id, action, remark) => request.put(`/admin/reports/${id}/review`, { action, remark }),

  // ===== 公告管理（全部管理员可用） =====
  /** 获取全部公告列表（含下线） */
  listAnnouncements: () => request.get('/admin/announcements'),
  /** 新增公告 */
  createAnnouncement: (data) => request.post('/admin/announcements', data),
  /** 更新公告 */
  updateAnnouncement: (id, data) => request.put(`/admin/announcements/${id}`, data),
  /** 上线/下线公告 */
  updateAnnouncementStatus: (id, status) => request.put(`/admin/announcements/${id}/status`, null, { params: { status } }),
  /** 删除公告 */
  deleteAnnouncement: (id) => request.delete(`/admin/announcements/${id}`),

  // ===== 聊天管理（CHAT_MANAGE） =====
  /** 分页查询所有会话 */
  listChats: (params) => request.get('/admin/chats', { params }),
  /** 删除会话及其所有消息 */
  deleteChat: (id) => request.delete(`/admin/chats/${id}`),

  // ===== 交易管理（TRADE_MANAGE） =====
  /** 分页查询所有交易 */
  listTrades: (params) => request.get('/admin/trades', { params }),
  /** 强制终止交易 */
  terminateTrade: (id, reason) => request.put(`/admin/trades/${id}/terminate`, null, { params: { reason } }),
  /** 分页查询申诉列表（status: 0=待处理 1=已处理 2=已驳回） */
  listAppeals: (params) => request.get('/admin/appeals', { params }),
  /** 处理申诉：action=1 已处理  action=2 已驳回 */
  reviewAppeal: (id, action, remark) => request.put(`/admin/appeals/${id}/review`, { action, remark }),
}
