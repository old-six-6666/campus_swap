import request from '@/api/index'

export const chatApi = {
  /** 发送消息 */
  sendMessage: (data) => request.post('/chat/messages', data),

  /** 获取会话列表 */
  listConversations: (params) => request.get('/chat/conversations', { params }),

  /** 获取指定会话的消息历史 */
  listMessages: (convId, params) => request.get(`/chat/conversations/${convId}/messages`, { params }),

  /** 标记会话已读 */
  markRead: (convId) => request.put(`/chat/conversations/${convId}/read`),

  /** 获取未读消息总数 */
  getUnreadCount: () => request.get('/chat/unread', { silent: true }),
}
