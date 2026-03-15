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

  /** 终止/拒绝交易 */
  terminate: (tradeId, data) => request.post(`/trade/${tradeId}/terminate`, data || {}),

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
}
