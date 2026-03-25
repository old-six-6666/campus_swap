<template>
  <div class="post-detail-page">
    <!-- 顶部导航 -->
    <div class="page-header">
      <el-button type="primary" link :icon="ArrowLeft" @click="router.back()">返回广场</el-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <!-- 帖子不存在 -->
    <el-empty v-else-if="!post" description="动态不存在或已被删除" />

    <template v-else>
      <!-- 帖子主体 -->
      <el-card class="post-card">
        <!-- 用户信息 -->
        <div class="post-header">
          <el-avatar :size="44" :src="post.user?.avatar" class="avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="user-info">
            <span class="username">{{ post.user?.username || '匿名用户' }}</span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
          <el-tag :type="postTypeInfo.color" size="small" class="type-tag">
            {{ postTypeInfo.icon }} {{ postTypeInfo.text }}
          </el-tag>
        </div>

        <!-- 帖子内容 -->
        <div class="post-content">{{ post.content }}</div>

        <!-- 帖子图片 -->
        <div v-if="post.images?.length" class="post-images" :class="`count-${Math.min(post.images.length, 9)}`">
          <el-image
            v-for="(img, idx) in post.images"
            :key="idx"
            :src="img"
            fit="cover"
            class="post-image"
            :preview-src-list="post.images"
            :initial-index="idx"
          >
            <template #error>
              <div class="post-image-error"><el-icon><Picture /></el-icon></div>
            </template>
          </el-image>
        </div>

        <!-- 关联物品卡片 -->
        <div v-if="post.item" class="item-card" @click="router.push(`/item/${post.item.id}`)">
          <el-image
            v-if="post.item.coverImage"
            :src="post.item.coverImage"
            fit="cover"
            class="item-cover"
          >
            <template #error>
              <div class="item-cover-placeholder"><el-icon size="28"><Picture /></el-icon></div>
            </template>
          </el-image>
          <div v-else class="item-cover-placeholder"><el-icon size="28"><Picture /></el-icon></div>
          <div class="item-info">
            <div class="item-title">{{ post.item.title }}</div>
            <div class="item-meta">
              <el-tag size="small" type="info">{{ post.item.category || '其他' }}</el-tag>
              <span class="item-price">¥ {{ post.item.price }}</span>
            </div>
            <div v-if="post.item.description" class="item-desc">{{ post.item.description }}</div>
          </div>
          <div class="item-link-hint"><el-icon><ArrowRight /></el-icon></div>
        </div>

        <!-- 标签 -->
        <div v-if="post.tags?.length" class="post-tags">
          <el-tag v-for="tag in post.tags" :key="tag" size="small" type="info" class="tag">
            # {{ tag }}
          </el-tag>
        </div>

        <!-- 互动统计 -->
        <div class="stats-row">
          <span class="stat-item"><el-icon><CircleCheck /></el-icon> {{ likeCount }} 点赞</span>
          <span class="stat-item"><el-icon><Star /></el-icon> {{ favoriteCount }} 收藏</span>
          <span class="stat-item"><el-icon><ChatDotRound /></el-icon> {{ comments.length }} 评论</span>
        </div>

        <!-- 互动按钮 -->
        <div class="action-row">
          <el-button
            type="primary" link
            :class="{ 'is-active': isLiked }"
            :icon="CircleCheck"
            @click="handleLike"
            class="action-btn"
          >{{ isLiked ? '已赞' : '点赞' }}</el-button>

          <el-button
            type="warning" link
            :class="{ 'is-active': isFavorited }"
            :icon="Star"
            @click="handleFavorite"
            class="action-btn"
          >{{ isFavorited ? '已收藏' : '收藏' }}</el-button>

          <el-button type="info" link :icon="Share" @click="handleShare" class="action-btn">分享</el-button>
        </div>
      </el-card>

      <!-- 评论区 -->
      <el-card class="comment-card">
        <template #header>
          <span class="comment-title"><el-icon><ChatDotRound /></el-icon> 全部评论（{{ comments.length }}）</span>
        </template>

        <!-- 发评论 -->
        <div class="comment-input-wrap">
          <el-avatar :size="36" :src="userStore.userInfo?.avatar" class="input-avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="input-area">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="2"
              placeholder="写下你的评论...（输入 @问一问 可向AI提问）"
              maxlength="500"
              show-word-limit
            />
            <div class="input-actions">
              <div class="emoji-wrap">
                <el-button type="info" link size="small" @click="showCommentEmoji = !showCommentEmoji">😊 表情</el-button>
                <EmojiPicker
                  v-if="showCommentEmoji"
                  class="emoji-picker-popup"
                  :native="true"
                  :disable-skin-tones="true"
                  :hide-search="false"
                  @select="onSelectCommentEmoji"
                />
              </div>
              <el-button
                type="primary"
                size="small"
                :loading="postingComment"
                :disabled="!commentContent.trim()"
                @click="handleComment"
              >发表评论</el-button>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 评论列表 -->
        <div v-if="commentsLoading" class="loading-wrap">
          <el-skeleton :rows="3" animated />
        </div>

        <el-empty v-else-if="!comments.length" description="暂无评论，来抢沙发吧~" :image-size="80" />

        <div v-else class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <el-avatar :size="36" :src="comment.user?.avatar" class="comment-avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="comment-body">
              <div class="comment-meta">
                <span class="comment-username">{{ comment.user?.username || '匿名用户' }}</span>
                <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                <el-button
                  v-if="userStore.userInfo?.id === comment.userId"
                  type="danger" link size="small"
                  @click="handleDeleteComment(comment.id)"
                  class="delete-btn"
                >删除</el-button>
              </div>
              <div class="comment-content">{{ comment.content }}</div>

              <!-- 回复按钮 -->
              <el-button
                type="primary" link size="small"
                @click="toggleReplyInput(comment.id, comment.user?.username)"
                class="reply-btn"
              >回复</el-button>

              <!-- 子评论（回复列表） -->
              <div v-if="comment.replies?.length" class="reply-list">
                <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                  <el-avatar :size="28" :src="reply.user?.avatar" class="reply-avatar" :class="{ 'ai-avatar': reply.userId === AI_USER_ID }">
                    <el-icon v-if="reply.userId === AI_USER_ID"><Cpu /></el-icon>
                    <el-icon v-else><User /></el-icon>
                  </el-avatar>
                  <div class="reply-body" :class="{ 'ai-reply': reply.userId === AI_USER_ID }">
                    <div class="comment-meta">
                      <span class="comment-username" :class="{ 'ai-username': reply.userId === AI_USER_ID }">
                        {{ reply.user?.username || '匿名用户' }}
                        <el-tag v-if="reply.userId === AI_USER_ID" size="small" type="primary" effect="plain" class="ai-tag">AI</el-tag>
                      </span>
                      <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                      <el-button
                        v-if="userStore.userInfo?.id === reply.userId"
                        type="danger" link size="small"
                        @click="handleDeleteComment(reply.id)"
                        class="delete-btn"
                      >删除</el-button>
                    </div>
                    <div
                      class="comment-content"
                      :class="{ 'ai-markdown': reply.userId === AI_USER_ID }"
                      v-html="reply.userId === AI_USER_ID
                        ? (typingTexts[reply.id] !== undefined
                            ? '<span style=\'white-space:pre-wrap\'>' + typingTexts[reply.id] + '<span class=\'typing-cursor\'>▋</span></span>'
                            : renderMarkdown(reply.content))
                        : reply.content"
                    ></div>
                    <!-- AI 回复下方提示可继续对话 -->
                    <div v-if="reply.userId === AI_USER_ID" class="ai-continue-hint">
                      💬 点击「回复」可继续追问
                    </div>
                    <!-- 回复某条子评论：parentId 仍指向一级评论，但 @ 被回复人 -->
                    <el-button
                      type="primary" link size="small"
                      @click="toggleReplyInput(comment.id, reply.user?.username)"
                      class="reply-btn"
                    >{{ reply.userId === AI_USER_ID ? '继续提问' : '回复' }}</el-button>
                  </div>
                </div>
              </div>

              <!-- 回复输入框（在回复列表下方展开） -->
              <div v-if="replyTarget?.commentId === comment.id" class="reply-input-wrap">
                <el-input
                  v-model="replyContent"
                  type="textarea"
                  :rows="2"
                  :placeholder="replyTarget.username ? `回复 @${replyTarget.username}...` : `回复 ${comment.user?.username || '匿名用户'}...`"
                  maxlength="500"
                  show-word-limit
                />
                <div class="reply-actions">
                  <div class="emoji-wrap">
                    <el-button type="info" link size="small" @click="showReplyEmoji = !showReplyEmoji">😊 表情</el-button>
                    <EmojiPicker
                      v-if="showReplyEmoji"
                      class="emoji-picker-popup"
                      :native="true"
                      :disable-skin-tones="true"
                      :hide-search="false"
                      @select="onSelectReplyEmoji"
                    />
                  </div>
                  <el-button size="small" @click="replyTarget = null; showReplyEmoji = false">取消</el-button>
                  <el-button
                    type="primary" size="small"
                    :loading="postingReply"
                    :disabled="!replyContent.trim()"
                    @click="handleReply(comment.id)"
                  >回复</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, User, Picture, CircleCheck, Star, ChatDotRound, Share, Cpu } from '@element-plus/icons-vue'
import EmojiPicker from 'vue3-emoji-picker'
import 'vue3-emoji-picker/css'
import MarkdownIt from 'markdown-it'
import { squareApi } from '@/api/modules/square'
import { useUserStore } from '@/stores/useUserStore'

const md = new MarkdownIt({ breaks: true, linkify: false })

function renderMarkdown(content) {
  if (!content) return ''
  return md.render(content)
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const AI_USER_ID = 999999999

const postId = computed(() => Number(route.params.id))

const loading = ref(false)
const post = ref(null)
const isLiked = ref(false)
const isFavorited = ref(false)
const likeCount = ref(0)
const favoriteCount = ref(0)

const commentsLoading = ref(false)
const comments = ref([])

const commentContent = ref('')
const postingComment = ref(false)
// replyTarget: { commentId, username } — commentId 是一级评论 id，username 是被回复人
const replyTarget = ref(null)
const replyContent = ref('')
const postingReply = ref(false)

// AI 轮询 & 打字动画
let aiPollTimer = null
let aiPollCount = 0
const AI_POLL_MAX = 20       // 最多轮询 20 次（40秒）
const AI_POLL_INTERVAL = 2000
// key: replyId, value: 正在打字显示的文本
const typingTexts = ref({})

function getKnownAiReplyIds() {
  const ids = new Set()
  for (const c of comments.value) {
    for (const r of (c.replies || [])) {
      if (r.userId === AI_USER_ID) ids.add(r.id)
    }
  }
  return ids
}

function startAiPolling() {
  stopAiPolling()
  const knownIds = getKnownAiReplyIds()
  aiPollCount = 0
  aiPollTimer = setInterval(async () => {
    aiPollCount++
    if (aiPollCount > AI_POLL_MAX) { stopAiPolling(); return }
    try {
      const res = await squareApi.getComments(postId.value)
      const newComments = res || []
      // 找出新增的 AI 回复
      const newAiReplies = []
      for (const c of newComments) {
        for (const r of (c.replies || [])) {
          if (r.userId === AI_USER_ID && !knownIds.has(r.id)) {
            newAiReplies.push(r)
            knownIds.add(r.id)
          }
        }
      }
      if (newAiReplies.length > 0) {
        comments.value = newComments
        stopAiPolling()
        // 对每条新 AI 回复做打字动画
        for (const r of newAiReplies) {
          playTypingAnimation(r.id, r.content)
        }
      }
    } catch { /* 忽略轮询中的网络错误 */ }
  }, AI_POLL_INTERVAL)
}

function stopAiPolling() {
  if (aiPollTimer) { clearInterval(aiPollTimer); aiPollTimer = null }
}

function playTypingAnimation(replyId, fullText) {
  typingTexts.value[replyId] = ''
  let i = 0
  const tick = setInterval(() => {
    i++
    typingTexts.value[replyId] = fullText.slice(0, i)
    if (i >= fullText.length) {
      clearInterval(tick)
      // 动画结束后删除 key，让模板切换回 markdown 渲染
      delete typingTexts.value[replyId]
    }
  }, 18)
}

// emoji picker 控制
const showCommentEmoji = ref(false)
const showReplyEmoji = ref(false)

function onSelectCommentEmoji(emoji) {
  commentContent.value += emoji.i
  showCommentEmoji.value = false
}

function onSelectReplyEmoji(emoji) {
  replyContent.value += emoji.i
  showReplyEmoji.value = false
}

function handleClickOutside(e) {
  if (!e.target.closest('.emoji-wrap')) {
    showCommentEmoji.value = false
    showReplyEmoji.value = false
  }
}

const postTypeMap = {
  1: { text: '发布了一个物品', color: 'primary', icon: '📦' },
  2: { text: '换物成功', color: 'success', icon: '🎉' },
  3: { text: '分享动态', color: 'info', icon: '💬' },
  4: { text: '求换动态', color: 'warning', icon: '🔍' }
}
const postTypeInfo = computed(() => postTypeMap[post.value?.type] || { text: '动态', color: 'default', icon: '📝' })

function formatTime(time) {
  if (!time) return ''
  const now = new Date()
  const t = new Date(time)
  const diff = now - t
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return t.toLocaleDateString()
}

async function loadPost() {
  loading.value = true
  try {
    const res = await squareApi.getPostDetail(postId.value)
    post.value = res
    isLiked.value = res.isLiked || false
    isFavorited.value = res.isFavorited || false
    likeCount.value = res.likeCount || 0
    favoriteCount.value = res.favoriteCount || 0
  } catch {
    post.value = null
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  commentsLoading.value = true
  try {
    const res = await squareApi.getComments(postId.value)
    comments.value = res || []
  } finally {
    commentsLoading.value = false
  }
}

async function handleLike() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  const prev = isLiked.value
  isLiked.value = !isLiked.value
  likeCount.value += isLiked.value ? 1 : -1
  try {
    if (isLiked.value) await squareApi.likePost(postId.value)
    else await squareApi.unlikePost(postId.value)
  } catch {
    isLiked.value = prev
    likeCount.value += prev ? 1 : -1
  }
}

async function handleFavorite() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  const prev = isFavorited.value
  isFavorited.value = !isFavorited.value
  favoriteCount.value += isFavorited.value ? 1 : -1
  try {
    if (isFavorited.value) await squareApi.favoritePost(postId.value)
    else await squareApi.unfavoritePost(postId.value)
  } catch {
    isFavorited.value = prev
    favoriteCount.value += prev ? 1 : -1
  }
}

function handleShare() {
  const url = `${window.location.origin}/square/post/${postId.value}`
  navigator.clipboard.writeText(url)
    .then(() => ElMessage.success('链接已复制'))
    .catch(() => ElMessage.info(`分享链接: ${url}`))
  squareApi.sharePost(postId.value).catch(() => {})
}

async function handleComment() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  const content = commentContent.value.trim()
  postingComment.value = true
  try {
    await squareApi.addComment({ postId: postId.value, content }, { silent: true })
    commentContent.value = ''
    await loadComments()
    if (content.startsWith('@问一问')) {
      ElMessage.success('已向 AI 提问，等待回复中...')
      startAiPolling()
    } else {
      ElMessage.success('评论成功')
    }
  } catch (e) {
    const msg = e?.message || ''
    if (msg.includes('违规')) {
      ElMessage.error(msg)
      commentContent.value = ''
    } else {
      ElMessage.error('评论失败')
    }
  } finally {
    postingComment.value = false
  }
}

function toggleReplyInput(commentId, username) {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  if (replyTarget.value?.commentId === commentId && replyTarget.value?.username === username) {
    replyTarget.value = null
  } else {
    replyTarget.value = { commentId, username: username || null }
  }
  replyContent.value = ''
}

async function handleReply(parentId) {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  postingReply.value = true
  const isAiConversation = replyTarget.value?.username === '问一问'
  try {
    await squareApi.addComment({ postId: postId.value, content: replyContent.value.trim(), parentId }, { silent: true })
    replyContent.value = ''
    replyTarget.value = null
    await loadComments()
    if (isAiConversation) {
      ElMessage.success('已向 AI 追问，等待回复中...')
      startAiPolling()
    } else {
      ElMessage.success('回复成功')
    }
  } catch (e) {
    const msg = e?.message || ''
    if (msg.includes('违规')) {
      ElMessage.error(msg)
      replyContent.value = ''
    } else {
      ElMessage.error('回复失败')
    }
  } finally {
    postingReply.value = false
  }
}

async function handleDeleteComment(commentId) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
    await squareApi.deleteComment(commentId)
    await loadComments()
    ElMessage.success('删除成功')
  } catch {
    // 取消删除
  }
}

onMounted(() => {
  loadPost()
  loadComments()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  stopAiPolling()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.post-detail-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 16px;

  .page-header {
    margin-bottom: 16px;
  }

  .loading-wrap {
    padding: 24px 0;
  }

  .post-card {
    margin-bottom: 16px;
    border-radius: 12px;

    .post-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;

      .avatar { flex-shrink: 0; }

      .user-info {
        flex: 1;
        display: flex;
        flex-direction: column;
        .username { font-weight: 600; font-size: 15px; color: #1a2e35; }
        .post-time { font-size: 12px; color: #8fa8b2; margin-top: 2px; }
      }
    }

    .post-content {
      font-size: 15px;
      line-height: 1.7;
      color: #1a2e35;
      white-space: pre-wrap;
      margin-bottom: 12px;
    }

    .post-images {
      display: grid;
      gap: 4px;
      margin-bottom: 16px;

      &.count-1 {
        grid-template-columns: 1fr;
        .post-image { height: 300px; border-radius: 8px; }
      }
      &.count-2, &.count-4 {
        grid-template-columns: repeat(2, 1fr);
        .post-image { height: 180px; border-radius: 6px; }
      }
      &.count-3, &.count-5, &.count-6,
      &.count-7, &.count-8, &.count-9 {
        grid-template-columns: repeat(3, 1fr);
        .post-image { height: 150px; border-radius: 6px; }
      }

      .post-image {
        width: 100%;
        object-fit: cover;
        cursor: pointer;
      }

      .post-image-error {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #F4F7F8;
        color: #8fa8b2;
      }
    }

    .post-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-bottom: 16px;
    }

    .stats-row {
      display: flex;
      gap: 20px;
      padding: 12px 0;
      border-top: 1px solid #f0f2f5;
      border-bottom: 1px solid #f0f2f5;
      margin-bottom: 12px;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: #8fa8b2;
      }
    }

    .item-card {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 12px 0;
      padding: 12px;
      border: 1px solid #ebeef5;
      border-radius: 8px;
      cursor: pointer;
      transition: box-shadow 0.2s;

      &:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }

      .item-cover {
        width: 80px;
        height: 80px;
        border-radius: 6px;
        flex-shrink: 0;
        object-fit: cover;
      }

      .item-cover-placeholder {
        width: 80px;
        height: 80px;
        border-radius: 6px;
        flex-shrink: 0;
        background: #F4F7F8;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #8fa8b2;
      }

      .item-info {
        flex: 1;
        min-width: 0;

        .item-title {
          font-weight: 600;
          font-size: 14px;
          color: #1a2e35;
          margin-bottom: 6px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .item-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 4px;

          .item-price { font-size: 14px; color: #F1C65E; font-weight: 600; }
        }

        .item-desc {
          font-size: 12px;
          color: #8fa8b2;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .item-link-hint { color: #8fa8b2; flex-shrink: 0; }
    }

    .action-row {
      display: flex;
      gap: 8px;

      .action-btn {
        color: #8fa8b2;
        &.is-active { font-weight: 600; }
        &.el-button--primary.is-active { color: #1B99AA; }
        &.el-button--warning.is-active { color: #e6a23c; }
      }
    }
  }

  .comment-card {
    border-radius: 12px;
    overflow: visible;

    :deep(.el-card__body) {
      overflow: visible;
    }

    .comment-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 15px;
      font-weight: 600;
    }

    .comment-input-wrap {
      display: flex;
      gap: 10px;
      align-items: flex-start;

      .input-avatar { flex-shrink: 0; margin-top: 4px; }

      .input-area {
        flex: 1;
        .input-actions {
          display: flex;
          justify-content: flex-end;
          align-items: center;
          gap: 8px;
          margin-top: 8px;
        }
      }
    }

    .emoji-wrap {
      position: relative;

      .emoji-picker-popup {
        position: absolute;
        bottom: 32px;
        right: 0;
        z-index: 9999;
        box-shadow: 0 4px 20px rgba(0,0,0,0.15);
        border-radius: 10px;
        overflow: hidden;
      }
    }

    .comment-list {
      display: flex;
      flex-direction: column;
      gap: 20px;

      .comment-item {
        display: flex;
        gap: 10px;

        .comment-avatar { flex-shrink: 0; margin-top: 2px; }

        .comment-body {
          flex: 1;

          .comment-meta {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 4px;

            .comment-username { font-weight: 600; font-size: 13px; color: #1a2e35; }
            .comment-time { font-size: 12px; color: #8fa8b2; }
            .delete-btn { margin-left: auto; }
          }

          .comment-content {
            font-size: 14px;
            color: #606266;
            line-height: 1.6;
          }

          .reply-btn { margin-top: 4px; font-size: 12px; }

          .reply-input-wrap {
            margin-top: 8px;
            .reply-actions {
              display: flex;
              justify-content: flex-end;
              align-items: center;
              gap: 8px;
              margin-top: 6px;
            }
          }

          .reply-list {
            margin-top: 10px;
            padding: 10px 12px;
            background: #f7f8fa;
            border-radius: 8px;
            display: flex;
            flex-direction: column;
            gap: 12px;

            .reply-item {
              display: flex;
              gap: 8px;

              .reply-avatar {
                flex-shrink: 0;
                margin-top: 2px;
                &.ai-avatar {
                  background: linear-gradient(135deg, #667eea, #764ba2);
                  color: #fff;
                }
              }
              .reply-body {
                flex: 1;
                &.ai-reply {
                  background: linear-gradient(135deg, #f0f4ff, #f5f0ff);
                  border-radius: 8px;
                  padding: 8px 10px;
                  border-left: 3px solid #667eea;
                }
              }
            }
          }

          .ai-username {
            color: #667eea;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 4px;
            .ai-tag { margin-left: 2px; }
          }

          .ai-continue-hint {
            font-size: 11px;
            color: #a0a8c0;
            margin-top: 4px;
          }

          :deep(.typing-cursor) {
            display: inline-block;
            animation: blink 0.7s step-end infinite;
          }
          @keyframes blink {
            0%, 100% { opacity: 1; }
            50% { opacity: 0; }
          }

          .ai-markdown {
            line-height: 1.7;
            :deep(p) { margin: 4px 0; }
            :deep(strong) { font-weight: 700; color: #1a2e35; }
            :deep(ul), :deep(ol) { padding-left: 18px; margin: 4px 0; }
            :deep(li) { margin: 2px 0; }
            :deep(h1), :deep(h2), :deep(h3) { font-size: 14px; font-weight: 700; margin: 6px 0 2px; }
            :deep(code) { background: #f0f2f5; padding: 1px 4px; border-radius: 3px; font-size: 12px; }
          }
        }
      }
    }
  }
}
</style>
