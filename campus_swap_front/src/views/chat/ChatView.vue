<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatApi } from '@/api/modules/chat'
import { notificationApi } from '@/api/modules/notification'
import { userApi } from '@/api/modules/user'
import { useUserStore } from '@/stores/useUserStore'
import { showInfo } from '@/utils/notify'
import { ChatLineRound, ShoppingBag, Search, Plus, Bell } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ── 会话列表 ────────────────────────────────────────────────────────────
const conversations = ref([])
const loadingConvs = ref(false)
const currentConv = ref(null)   // 当前选中的会话

// ── 消息列表 ────────────────────────────────────────────────────────────
const messages = ref([])
const loadingMsgs = ref(false)
const newMsg = ref('')
const sending = ref(false)
const messagesEl = ref(null)

// ── 通知 ──────────────────────────────────────────────────────────────────
const activeTab = ref('chat')   // 'chat' | 'notification'
const notifications = ref([])
const notifUnread = ref(0)
const notifLoading = ref(false)

async function fetchNotifications() {
  notifLoading.value = true
  try {
    notifications.value = await notificationApi.list({ page: 1, size: 50 })
  } finally {
    notifLoading.value = false
  }
}

async function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'notification') {
    await fetchNotifications()
    if (notifUnread.value > 0) {
      await notificationApi.markAllRead().catch(() => {})
      notifUnread.value = 0
    }
  }
}

async function fetchNotifUnread() {
  try {
    const data = await notificationApi.getUnreadCount()
    notifUnread.value = data?.count || 0
  } catch { /* ignore */ }
}

function notifTypeLabel(type) {
  if (type === 'LIKE') return '赞了你的帖子'
  if (type === 'FAVORITE') return '收藏了你的帖子'
  if (type === 'COMMENT') return '评论了你的帖子'
  return ''
}

// ── 添加好友弹窗 ─────────────────────────────────────────────────────────
const addFriendDialog = ref(false)
const searchKw = ref('')
const searchResults = ref([])
const searching = ref(false)

const myId = computed(() => userStore.userInfo?.id)

// ── 获取会话列表 ──────────────────────────────────────────────────────────
async function fetchConversations() {
  loadingConvs.value = true
  try {
    const data = await chatApi.listConversations({ page: 1, size: 50 })
    conversations.value = data.records || []
  } finally {
    loadingConvs.value = false
  }
}

// ── 选中一个会话 ──────────────────────────────────────────────────────────
async function selectConv(conv) {
  currentConv.value = conv
  messages.value = []
  if (conv.conversationId) {
    await fetchMessages()
    if (conv.unreadCount > 0) {
      await chatApi.markRead(conv.conversationId).catch(() => {})
      conv.unreadCount = 0
    }
  }
}

// ── 获取消息历史 ──────────────────────────────────────────────────────────
async function fetchMessages() {
  if (!currentConv.value?.conversationId) return
  loadingMsgs.value = true
  try {
    const data = await chatApi.listMessages(currentConv.value.conversationId, { page: 1, size: 100 })
    messages.value = data.records || []
    await nextTick()
    scrollToBottom()
  } finally {
    loadingMsgs.value = false
  }
}

function scrollToBottom() {
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

// ── 发送消息 ──────────────────────────────────────────────────────────────
async function handleSend() {
  const content = newMsg.value.trim()
  if (!content || !currentConv.value) return
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  sending.value = true
  try {
    await chatApi.sendMessage({
      receiverId: currentConv.value.otherUserId,
      itemId: currentConv.value.itemId || 0,
      content,
    })
    newMsg.value = ''
    // 刷新会话列表
    await fetchConversations()
    // 若之前没有 conversationId（新会话），找到已创建的会话并选中
    if (!currentConv.value.conversationId) {
      const found = conversations.value.find(
        c => c.otherUserId === currentConv.value.otherUserId &&
             (c.itemId || 0) === (currentConv.value.itemId || 0)
      )
      if (found) {
        currentConv.value = found
      }
    } else {
      const found = conversations.value.find(c => c.conversationId === currentConv.value.conversationId)
      if (found) Object.assign(currentConv.value, { lastMsg: found.lastMsg, lastMsgTime: found.lastMsgTime })
    }
    await fetchMessages()
  } finally {
    sending.value = false
  }
}

// ── 搜索用户（添加好友） ──────────────────────────────────────────────────
async function doSearch() {
  if (!searchKw.value.trim()) return
  searching.value = true
  try {
    searchResults.value = await userApi.searchUsers(searchKw.value.trim())
    if (searchResults.value.length === 0) {
      showInfo('未找到匹配的用户')
    }
  } finally {
    searching.value = false
  }
}

// ── 开始和某用户聊天 ──────────────────────────────────────────────────────
async function openChat(user) {
  addFriendDialog.value = false
  searchKw.value = ''
  searchResults.value = []
  // 检查是否已有无商品关联的会话
  const existing = conversations.value.find(c => c.otherUserId === user.id && !c.itemId)
  if (existing) {
    await selectConv(existing)
  } else {
    currentConv.value = {
      conversationId: null,
      otherUserId: user.id,
      otherNickname: user.nickname,
      otherAvatar: user.avatar,
      itemId: 0,
      unreadCount: 0,
    }
    messages.value = []
  }
}

// ── 处理从商品详情页跳转过来的参数 ──────────────────────────────────────
async function handleQueryNav() {
  const toId = route.query.to ? Number(route.query.to) : null
  if (!toId) return
  const itemId = route.query.item ? Number(route.query.item) : 0
  const nickname = route.query.nickname || '对方'
  const avatar = route.query.avatar || null
  const itemTitle = route.query.itemTitle || null

  const existing = conversations.value.find(
    c => c.otherUserId === toId && (c.itemId || 0) === itemId
  )
  if (existing) {
    await selectConv(existing)
  } else {
    currentConv.value = {
      conversationId: null,
      otherUserId: toId,
      otherNickname: nickname,
      otherAvatar: avatar,
      itemId: itemId,
      itemTitle: itemTitle,
      unreadCount: 0,
    }
    messages.value = []
  }
}

// ── 交易链接解析：将消息中 /trade/{id} 分离为可点击链接 ──────────────────
function parseMsgParts(content) {
  if (!content) return [{ type: 'text', value: '' }]
  const tradeRe = /\/trade\/(\d+)/g
  const parts = []
  let last = 0
  let m
  while ((m = tradeRe.exec(content)) !== null) {
    if (m.index > last) parts.push({ type: 'text', value: content.slice(last, m.index) })
    parts.push({ type: 'trade', value: m[0], id: m[1] })
    last = m.index + m[0].length
  }
  if (last < content.length) parts.push({ type: 'text', value: content.slice(last) })
  return parts
}

// ── 时间格式化 ────────────────────────────────────────────────────────────
function formatTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diffMs = now - d
  const diffDays = Math.floor(diffMs / 86400000)
  if (diffDays === 0) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diffDays === 1) return '昨天'
  return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

function onCloseDialog() {
  searchKw.value = ''
  searchResults.value = []
}

onMounted(async () => {
  await fetchConversations()
  await handleQueryNav()
  fetchNotifUnread()
})

watch(() => route.query, async () => {
  await fetchConversations()
  await handleQueryNav()
})
</script>

<template>
  <div class="chat-page">
    <!-- 左栏：tab 切换 -->
    <div class="chat-left">
      <div class="left-header">
        <div class="tab-bar">
          <div class="tab-item" :class="{ active: activeTab === 'chat' }" @click="switchTab('chat')">
            聊天
          </div>
          <div class="tab-item" :class="{ active: activeTab === 'notification' }" @click="switchTab('notification')">
            通知
            <el-badge v-if="notifUnread > 0" :value="notifUnread" class="tab-badge" />
          </div>
        </div>
        <el-button v-if="activeTab === 'chat'" type="primary" size="small" :icon="Plus" @click="addFriendDialog = true">
          添加好友
        </el-button>
      </div>

      <!-- 聊天会话列表 -->
      <div v-if="activeTab === 'chat'" v-loading="loadingConvs" class="conv-list">
        <div v-if="conversations.length === 0 && !loadingConvs" class="empty-hint">
          暂无会话，点击「添加好友」开始聊天
        </div>
        <div
          v-for="conv in conversations"
          :key="conv.conversationId"
          class="conv-item"
          :class="{ active: currentConv?.conversationId === conv.conversationId }"
          @click="selectConv(conv)"
        >
          <el-avatar :size="42" :src="conv.otherAvatar" />
          <div class="conv-meta">
            <div class="conv-top">
              <span class="conv-name">{{ conv.otherNickname }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMsgTime) }}</span>
            </div>
            <div class="conv-bottom">
              <span class="conv-last">{{ conv.lastMsg || '暂无消息' }}</span>
              <el-badge v-if="conv.unreadCount > 0" :value="conv.unreadCount" class="unread-badge" />
            </div>
            <div v-if="conv.itemTitle" class="conv-item-tag">
              <el-icon><ShoppingBag /></el-icon>
              {{ conv.itemTitle }}
            </div>
          </div>
        </div>
      </div>

      <!-- 通知列表 -->
      <div v-else v-loading="notifLoading" class="notif-list">
        <div v-if="notifications.length === 0 && !notifLoading" class="empty-hint">
          暂无通知
        </div>
        <div v-for="n in notifications" :key="n.id" class="notif-item" :class="{ unread: !n.isRead }"
          @click="$router.push({ name: 'PostDetail', params: { id: n.postId } })">
          <el-avatar :size="36" :src="n.sender?.avatar" />
          <div class="notif-body">
            <span class="notif-name">{{ n.sender?.nickname }}</span>
            <span class="notif-action">{{ notifTypeLabel(n.type) }}</span>
            <div v-if="n.content" class="notif-content">{{ n.content }}</div>
            <div class="notif-time">{{ formatTime(n.createdAt) }}</div>
          </div>
          <span v-if="!n.isRead" class="unread-dot" />
        </div>
      </div>
    </div>

    <!-- 右栏：聊天区域 -->
    <div class="chat-right">
      <template v-if="currentConv">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <el-avatar :size="38" :src="currentConv.otherAvatar" />
          <div class="chat-header-info">
            <span class="other-name">{{ currentConv.otherNickname }}</span>
            <span v-if="currentConv.itemTitle" class="item-context">
              <el-icon><ShoppingBag /></el-icon>
              关于商品：{{ currentConv.itemTitle }}
            </span>
          </div>
        </div>

        <!-- 消息区 -->
        <div ref="messagesEl" v-loading="loadingMsgs" class="messages-area">
          <div v-if="messages.length === 0 && !loadingMsgs" class="no-msg">
            发送第一条消息开始对话吧 👋
          </div>
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="msg-row"
            :class="{ 'msg-me': msg.senderId === myId }"
          >
            <el-avatar :size="32" :src="msg.senderAvatar" class="msg-avatar" />
            <div class="msg-bubble">
              <div class="bubble-content">
                <template v-for="(part, i) in parseMsgParts(msg.content)" :key="i">
                  <span v-if="part.type === 'text'">{{ part.value }}</span>
                  <router-link
                    v-else
                    :to="{ name: 'TradeDetail', params: { id: part.id } }"
                    class="trade-link"
                  >📋 查看交易详情</router-link>
                </template>
              </div>
              <div class="bubble-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="input-area">
          <el-input
            v-model="newMsg"
            type="textarea"
            :rows="2"
            placeholder="输入消息，按 Enter 发送..."
            resize="none"
            @keydown.enter.exact.prevent="handleSend"
          />
          <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
        </div>
      </template>

      <!-- 未选择会话时的占位 -->
      <div v-else class="placeholder">
        <el-icon :size="64" color="#c0c4cc"><ChatLineRound /></el-icon>
        <p class="placeholder-title">选择一个会话开始聊天</p>
        <p class="placeholder-sub">或点击左上角「添加好友」搜索用户</p>
      </div>
    </div>

    <!-- 添加好友弹窗 -->
    <el-dialog
      v-model="addFriendDialog"
      title="添加好友"
      width="420px"
      @close="onCloseDialog"
    >
      <div class="search-bar">
        <el-input
          v-model="searchKw"
          placeholder="输入昵称搜索用户"
          clearable
          :prefix-icon="Search"
          @keyup.enter="doSearch"
        />
        <el-button type="primary" :loading="searching" @click="doSearch">搜索</el-button>
      </div>

      <div class="result-list">
        <div v-if="searchResults.length === 0 && !searching && searchKw" class="no-result">
          未找到用户，请换个关键词试试
        </div>
        <div v-for="user in searchResults" :key="user.id" class="result-item">
          <el-avatar :size="40" :src="user.avatar" />
          <div class="result-info">
            <span class="result-name">{{ user.nickname }}</span>
            <span v-if="user.school" class="result-school">{{ user.school }}</span>
          </div>
          <el-button size="small" type="primary" plain @click="openChat(user)">发消息</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.chat-page {
  display: flex;
  height: calc(100vh - 200px);
  min-height: 520px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.08);
}

/* ── 左栏 ──────────────────────────────────────────── */
.chat-left {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.left-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #ebeef5;

  .left-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}

.conv-list {
  flex: 1;
  overflow-y: auto;
}

.empty-hint {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 40px 16px;
  line-height: 1.6;
}

.conv-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;

  &:hover { background: #f2f6fc; }
  &.active { background: #ecf5ff; }
}

.conv-meta {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;

  .conv-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 140px;
  }

  .conv-time {
    font-size: 11px;
    color: #c0c4cc;
    flex-shrink: 0;
    margin-left: 4px;
  }
}

.conv-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .conv-last {
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 150px;
  }
}

.unread-badge {
  flex-shrink: 0;
}

.conv-item-tag {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── 右栏 ──────────────────────────────────────────── */
.chat-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  flex-shrink: 0;
}

.chat-header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .other-name {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  .item-context {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #909399;
  }
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: #f7f8fa;
}

.no-msg {
  text-align: center;
  color: #c0c4cc;
  font-size: 14px;
  margin-top: 60px;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;

  &.msg-me {
    flex-direction: row-reverse;

    .bubble-content {
      background: #409eff;
      color: #fff;
      border-radius: 16px 4px 16px 16px;
    }

    .bubble-time {
      text-align: right;
    }
  }
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-bubble {
  display: flex;
  flex-direction: column;
  max-width: 60%;
  gap: 3px;

  .bubble-content {
    background: #fff;
    color: #303133;
    padding: 10px 14px;
    border-radius: 4px 16px 16px 16px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

    .trade-link {
      display: inline-block;
      margin-top: 4px;
      padding: 4px 10px;
      background: #ecf5ff;
      color: #409eff;
      border-radius: 6px;
      text-decoration: none;
      font-size: 13px;
      border: 1px solid #d9ecff;
      &:hover { background: #d9ecff; }
    }
  }

  .bubble-time {
    font-size: 11px;
    color: #c0c4cc;
    padding: 0 4px;
  }
}

.input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid #ebeef5;
  background: #fff;
  flex-shrink: 0;
}

/* ── 占位区 ──────────────────────────────────────────── */
.placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;

  .placeholder-title {
    margin: 0;
    font-size: 16px;
    color: #909399;
  }

  .placeholder-sub {
    margin: 0;
    font-size: 13px;
    color: #c0c4cc;
  }
}

/* ── 添加好友弹窗 ──────────────────────────────────────── */
.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.result-list {
  max-height: 320px;
  overflow-y: auto;
}

.no-result {
  text-align: center;
  color: #909399;
  font-size: 14px;
  padding: 24px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child { border-bottom: none; }
}

.result-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;

  .result-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }

  .result-school {
    font-size: 12px;
    color: #909399;
  }
}

/* ── 通知列表 ──────────────────────────────────────────── */
.tab-bar {
  display: flex;
  gap: 4px;
  flex: 1;

  .tab-item {
    position: relative;
    padding: 4px 12px;
    font-size: 14px;
    font-weight: 500;
    color: #606266;
    cursor: pointer;
    border-radius: 6px;
    transition: all 0.15s;

    &:hover { background: #f0f2f5; }
    &.active { background: #ecf5ff; color: #409eff; }

    .tab-badge {
      position: absolute;
      top: 0;
      right: 0;
    }
  }
}

.notif-list {
  flex: 1;
  overflow-y: auto;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
  position: relative;

  &:hover { background: #f2f6fc; }
  &.unread { background: #fef9f0; }
}

.notif-body {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;

  .notif-name { font-weight: 600; color: #303133; margin-right: 4px; }
  .notif-content {
    color: #909399;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    margin-top: 2px;
  }
  .notif-time { font-size: 11px; color: #c0c4cc; margin-top: 2px; }
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
  margin-top: 6px;
}
</style>
