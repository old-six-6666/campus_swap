import request from '@/api/index'

export const notificationApi = {
  /** 获取通知列表 */
  list: (params) => request.get('/notification/list', { params }),

  /** 获取未读通知数 */
  getUnreadCount: () => request.get('/notification/unread-count', { silent: true }),

  /** 全部标记已读 */
  markAllRead: () => request.put('/notification/read-all'),
}
