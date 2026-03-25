import request from '@/api/index'

export const tradeApi = {
  /** 甲方发起交易申请 */
  initiate: (data) => request.post('/trade/initiate', data),

  /** 乙方确认匹配（同意交换） */
  match: (tradeId, data) => request.post(`/trade/${tradeId}/match`, data),

  /** 管理员审核 */
  audit: (tradeId, data) => request.post(`/trade/${tradeId}/audit`, data),

  /** 确认已发货 */
  deliver: (tradeId, data) => request.post(`/trade/${tradeId}/deliver`, data || {}),

  /** 确认已收货 */
  confirmReceipt: (tradeId) => request.post(`/trade/${tradeId}/confirm-receipt`),

  /** 申请终止交易（双方均申请后生效；PENDING_MATCH阶段或管理员直接终止） */
  terminate: (tradeId, data) => request.post(`/trade/${tradeId}/terminate`, data || {}),

  /** 撤回自己的终止申请 */
  cancelTerminate: (tradeId) => request.post(`/trade/${tradeId}/cancel-terminate`),

  /** 拒绝对方的终止申请 */
  rejectTerminate: (tradeId) => request.post(`/trade/${tradeId}/reject-terminate`),

  /** 管理员回滚状态 */
  rollback: (tradeId, data) => request.post(`/trade/${tradeId}/rollback`, data || {}),

  /** 获取交易详情 */
  getDetail: (tradeId) => request.get(`/trade/${tradeId}`),

  /** 获取我的交易列表，status 可选 */
  getMyTrades: (status) => request.get('/trade/my', { params: status ? { status } : {} }),

  /** 获取交易状态变更日志 */
  getLogs: (tradeId) => request.get(`/trade/${tradeId}/logs`),

  /** 查询物品当前活跃的交易（无需登录；silent=true 避免弹出错误提示） */
  getItemActiveTrade: (itemId) => request.get(`/trade/item/${itemId}`, { silent: true }),

  /** 获取需要当前用户操作的交易数量（"交易"菜单红点用） */
  getPendingCount: () => request.get('/trade/pending-count', { silent: true }),

  /** 提交申诉 */
  submitAppeal: (tradeId, data) => request.post(`/trade/${tradeId}/appeal`, data),

  /** 查询当前用户在该交易中的申诉列表 */
  getAppeals: (tradeId) => request.get(`/trade/${tradeId}/appeals`),
}
