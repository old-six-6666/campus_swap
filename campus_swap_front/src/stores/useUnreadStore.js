import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatApi } from '@/api/modules/chat'
import { notificationApi } from '@/api/modules/notification'

export const useUnreadStore = defineStore('unread', () => {
  const chatUnread = ref(0)
  const notifUnread = ref(0)

  /** 总未读数（导航栏红点用） */
  const total = () => chatUnread.value + notifUnread.value

  /** 从服务端重新拉取 */
  async function refresh() {
    const [chatData, notifData] = await Promise.all([
      chatApi.getUnreadCount().catch(() => null),
      notificationApi.getUnreadCount().catch(() => null),
    ])
    chatUnread.value = chatData?.totalUnread || 0
    notifUnread.value = notifData?.count || 0
  }

  /** 标记某会话已读后，本地减去对应未读数并刷新 */
  async function onConvRead(delta) {
    chatUnread.value = Math.max(0, chatUnread.value - delta)
    // 再向服务端确认一次（避免漂移）
    const data = await chatApi.getUnreadCount().catch(() => null)
    if (data != null) chatUnread.value = data.totalUnread || 0
  }

  /** 通知全部已读 */
  function clearNotif() {
    notifUnread.value = 0
  }

  return { chatUnread, notifUnread, total, refresh, onConvRead, clearNotif }
})
