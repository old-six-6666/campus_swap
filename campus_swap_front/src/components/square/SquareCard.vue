<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Star,
  ChatDotRound,
  More,
  User,
  Picture,
  Share,
  CircleCheck,
  Delete
} from '@element-plus/icons-vue'
import EmojiPicker from 'vue3-emoji-picker'
import 'vue3-emoji-picker/css'
import { squareApi } from '@/api/modules/square'
import { useUserStore } from '@/stores/useUserStore'

const props = defineProps({
  post: { type: Object, required: true }
})

const emit = defineEmits(['like-changed', 'favorite-changed', 'comment-added', 'post-deleted'])

const userStore = useUserStore()
const router = useRouter()
const isLiked = ref(props.post.isLiked || false)
const isFavorited = ref(props.post.isFavorited || false)
const likeCount = ref(props.post.likeCount || 0)
const favoriteCount = ref(props.post.favoriteCount || 0)
const commentCount = ref(props.post.commentCount || 0)
const showCommentInput = ref(false)
const commentContent = ref('')
const postingComment = ref(false)
const showCommentEmoji = ref(false)

function onSelectEmoji(emoji) {
  commentContent.value += emoji.i
  showCommentEmoji.value = false
}

const postTypeMap = {
  1: { text: '发布了一个物品', color: 'primary', icon: '📦' },
  2: { text: '换物成功', color: 'success', icon: '🎉' },
  3: { text: '分享动态', color: 'info', icon: '💬' }
}

const postTypeInfo = computed(() => postTypeMap[props.post.type] || { text: '动态', color: 'default', icon: '📝' })

const formatTime = (time) => {
  const now = new Date()
  const postTime = new Date(time)
  const diff = now - postTime
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return postTime.toLocaleDateString()
}

async function handleLike() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  const prevLiked = isLiked.value
  const prevCount = likeCount.value
  isLiked.value = !isLiked.value
  likeCount.value += isLiked.value ? 1 : -1
  try {
    if (isLiked.value) await squareApi.likePost(props.post.id, { silent: true })
    else await squareApi.unlikePost(props.post.id, { silent: true })
    emit('like-changed', { postId: props.post.id, liked: isLiked.value, count: likeCount.value })
  } catch {
    isLiked.value = prevLiked
    likeCount.value = prevCount
  }
}

async function handleFavorite() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  const prevFavorited = isFavorited.value
  const prevCount = favoriteCount.value
  isFavorited.value = !isFavorited.value
  favoriteCount.value += isFavorited.value ? 1 : -1
  try {
    if (isFavorited.value) await squareApi.favoritePost(props.post.id, { silent: true })
    else await squareApi.unfavoritePost(props.post.id, { silent: true })
    emit('favorite-changed', { postId: props.post.id, favorited: isFavorited.value, count: favoriteCount.value })
  } catch {
    isFavorited.value = prevFavorited
    favoriteCount.value = prevCount
  }
}

async function handleComment() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  if (!commentContent.value.trim()) { ElMessage.warning('评论内容不能为空'); return }
  postingComment.value = true
  try {
    await squareApi.addComment({ postId: props.post.id, content: commentContent.value.trim() })
    commentCount.value++
    commentContent.value = ''
    showCommentInput.value = false
    emit('comment-added', { postId: props.post.id })
    ElMessage.success('评论发表成功')
  } catch (error) {
    console.error('发表评论失败:', error)
  } finally {
    postingComment.value = false
  }
}

function handleShare() {
  const shareUrl = `${window.location.origin}/square/post/${props.post.id}`
  navigator.clipboard.writeText(shareUrl)
    .then(() => ElMessage.success('链接已复制到剪贴板'))
    .catch(() => ElMessage.info(`分享链接: ${shareUrl}`))
  squareApi.sharePost(props.post.id).catch(() => {})
}

function handleAction(command) {
  if (command === 'delete') handleDelete()
}

async function handleDelete() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  try {
    await ElMessageBox.confirm('确定要删除这条动态吗？删除后无法恢复。', '确认删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await squareApi.deletePost(props.post.id)
    ElMessage.success('动态删除成功')
    emit('post-deleted', props.post.id)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除动态失败:', error)
      ElMessage.error('删除动态失败')
    }
  }
}

const isCurrentUserPost = computed(() =>
  userStore.isLoggedIn && userStore.userInfo?.id === props.post.userId
)
</script>

<template>
  <div class="square-card">
    <!-- 用户信息行 -->
    <div class="card-header">
      <el-avatar
        :size="42"
        :src="userStore.getAvatar(post.user?.avatar)"
        class="user-avatar clickable"
        style="object-fit:cover"
        @click.stop="router.push({ name: 'UserHome', params: { id: post.userId } })"
      >
        <el-icon><User /></el-icon>
      </el-avatar>
      <div class="user-meta">
        <span
          class="username clickable"
          @click.stop="router.push({ name: 'UserHome', params: { id: post.userId } })"
        >{{ post.user?.username || '匿名用户' }}</span>
        <div class="post-meta">
          <span class="post-type-badge">{{ postTypeInfo.icon }} {{ postTypeInfo.text }}</span>
          <span class="post-time">· {{ formatTime(post.createdAt) }}</span>
        </div>
      </div>
      <div class="card-more">
        <el-dropdown v-if="isCurrentUserPost" trigger="click" @command="handleAction">
          <el-button type="info" link :icon="More" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="delete" style="color: #e74c6a;">
                <el-icon><Delete /></el-icon>
                删除动态
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 动态内容 -->
    <div class="card-content clickable" @click="router.push({ name: 'PostDetail', params: { id: post.id } })">
      <p v-if="post.content" class="content-text">{{ post.content }}</p>

      <!-- 图片组 -->
      <div v-if="post.images?.length" class="post-images" :class="`count-${Math.min(post.images.length, 9)}`">
        <el-image
          v-for="(img, idx) in post.images"
          :key="idx"
          :src="img"
          fit="cover"
          class="post-img"
          :preview-src-list="post.images"
          :initial-index="idx"
          @click.stop
        >
          <template #error>
            <div class="img-error"><el-icon><Picture /></el-icon></div>
          </template>
        </el-image>
      </div>

      <!-- 物品卡片 -->
      <div v-if="post.type === 1 && post.item" class="item-embed">
        <el-image
          v-if="post.item.coverImage"
          :src="post.item.coverImage"
          fit="cover"
          class="embed-img"
          :preview-src-list="[post.item.coverImage]"
          @click.stop
        >
          <template #error>
            <div class="img-error"><el-icon size="24"><Picture /></el-icon></div>
          </template>
        </el-image>
        <div class="embed-info">
          <h4 class="embed-title">{{ post.item.title }}</h4>
          <div class="embed-meta">
            <el-tag size="small">{{ post.item.condition || '未知成色' }}</el-tag>
            <span class="embed-price">¥ {{ post.item.price }}</span>
          </div>
          <p v-if="post.item.description" class="embed-desc">{{ post.item.description }}</p>
          <div v-if="post.tags?.length" class="embed-tags">
            <el-tag v-for="tag in post.tags" :key="tag.id" size="small" class="embed-tag">
              #{{ tag.name }}
            </el-tag>
          </div>
          <div v-if="post.item.exchangeFor" class="exchange-for">
            <span class="ex-label">想换：</span>
            <span>{{ post.item.exchangeFor }}</span>
          </div>
        </div>
      </div>

      <!-- 换物成功 -->
      <div v-if="post.type === 2 && post.swapRecord" class="swap-success">
        <span class="swap-emoji">🎉</span>
        <div class="swap-info">
          <p>成功与 <strong>{{ post.swapRecord.partnerName }}</strong> 完成换物</p>
          <div class="swap-items">
            <span class="swap-item-tag">{{ post.swapRecord.itemATitle }}</span>
            <span class="swap-arrow">↔</span>
            <span class="swap-item-tag">{{ post.swapRecord.itemBTitle }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 互动区 -->
    <div class="interaction-bar">
      <div class="stats-row">
        <span class="stat"><el-icon><CircleCheck /></el-icon> {{ likeCount }}</span>
        <span class="stat"><el-icon><Star /></el-icon> {{ favoriteCount }}</span>
        <span class="stat"><el-icon><ChatDotRound /></el-icon> {{ commentCount }}</span>
      </div>

      <div class="action-row">
        <button class="action-btn" :class="{ active: isLiked }" @click="handleLike">
          <el-icon><CircleCheck /></el-icon>
          <span>{{ isLiked ? '已赞' : '点赞' }}</span>
        </button>
        <button class="action-btn fav" :class="{ active: isFavorited }" @click="handleFavorite">
          <el-icon><Star /></el-icon>
          <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
        </button>
        <button class="action-btn" @click="showCommentInput = !showCommentInput">
          <el-icon><ChatDotRound /></el-icon>
          <span>评论</span>
        </button>
        <button class="action-btn" @click="handleShare">
          <el-icon><Share /></el-icon>
          <span>分享</span>
        </button>
      </div>
    </div>

    <!-- 评论输入 -->
    <div v-if="showCommentInput" class="comment-area">
      <el-input
        v-model="commentContent"
        type="textarea"
        :rows="2"
        placeholder="写下你的评论..."
        maxlength="500"
        show-word-limit
      />
      <div class="comment-actions">
        <div class="emoji-wrap">
          <el-button type="info" link size="small" @click="showCommentEmoji = !showCommentEmoji">😊 表情</el-button>
          <EmojiPicker
            v-if="showCommentEmoji"
            class="emoji-picker-popup"
            :native="true"
            :disable-skin-tones="true"
            @select="onSelectEmoji"
          />
        </div>
        <el-button size="small" @click="showCommentInput = false; showCommentEmoji = false">取消</el-button>
        <el-button type="primary" size="small" :loading="postingComment" @click="handleComment">
          发表评论
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.square-card {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 20px 22px;
  margin-bottom: 14px;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;
  transition: $transition-base;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
    border-color: $primary-light;
  }
}

/* ── 头部 ────────────────────── */
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.user-avatar { flex-shrink: 0; cursor: pointer; transition: $transition-fast; &:hover { opacity: 0.85; } }

.clickable { cursor: pointer; }

.user-meta {
  flex: 1;
  min-width: 0;

  .username {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
    letter-spacing: $letter-spacing-base;
    display: inline-block;
    transition: color 0.2s;
    &:hover { color: $primary; }
  }

  .post-meta {
    font-size: 12px;
    color: $text-secondary;
    margin-top: 2px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
  }

  .post-type-badge {
    color: $primary;
    font-weight: 500;
    font-size: 12px;
  }

  .post-time { color: $text-secondary; }
}

.card-more { margin-left: auto; }

/* ── 内容 ────────────────────── */
.card-content {
  margin-bottom: 14px;
  cursor: pointer;
}

.content-text {
  font-size: 15px;
  line-height: 1.65;
  color: $text-primary;
  margin-bottom: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  letter-spacing: $letter-spacing-base;
}

/* 图片组 */
.post-images {
  display: grid;
  gap: 4px;
  margin-bottom: 12px;
  border-radius: $border-radius-sm;
  overflow: hidden;

  &.count-1 {
    grid-template-columns: 1fr;
    .post-img { height: 220px; }
  }
  &.count-2, &.count-4 {
    grid-template-columns: repeat(2, 1fr);
    .post-img { height: 150px; }
  }
  &.count-3, &.count-5, &.count-6,
  &.count-7, &.count-8, &.count-9 {
    grid-template-columns: repeat(3, 1fr);
    .post-img { height: 120px; }
  }

  .post-img {
    width: 100%;
    object-fit: cover;
    cursor: pointer;
    border-radius: 8px;
  }
}

.img-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-subtle;
  color: $text-secondary;
}

/* 物品嵌入卡 */
.item-embed {
  border: 1px solid $border-color;
  border-radius: $border-radius-sm;
  overflow: hidden;
  background: $bg-subtle;
  margin-bottom: 4px;

  .embed-img {
    width: 100%;
    height: 180px;
    object-fit: cover;
  }

  .embed-info {
    padding: 14px 16px;

    .embed-title {
      font-size: 15px;
      font-weight: 600;
      color: $text-primary;
      margin-bottom: 8px;
      letter-spacing: $letter-spacing-base;
    }

    .embed-meta {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;

      .embed-price {
        font-size: 17px;
        font-weight: 700;
        color: $warning;
      }
    }

    .embed-desc {
      font-size: 13px;
      color: $text-regular;
      line-height: 1.5;
      margin-bottom: 8px;
    }

    .embed-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-bottom: 8px;

      .embed-tag {
        background: rgba(27, 153, 170, 0.1) !important;
        color: $primary !important;
        font-size: 11px;
      }
    }

    .exchange-for {
      padding: 8px 12px;
      background: rgba(241, 198, 94, 0.1);
      border-radius: 10px;
      border-left: 3px solid $warning;
      font-size: 13px;

      .ex-label { font-weight: 600; color: #8a6200; }
    }
  }
}

/* 换物成功 */
.swap-success {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: rgba(27, 153, 170, 0.06);
  border-radius: $border-radius-sm;
  border: 1px solid rgba(27, 153, 170, 0.15);

  .swap-emoji { font-size: 28px; flex-shrink: 0; }

  .swap-info {
    p { font-size: 14px; color: $text-primary; margin-bottom: 8px; font-weight: 500; }

    .swap-items {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .swap-item-tag {
        padding: 3px 10px;
        background: #fff;
        border-radius: 50px;
        font-size: 13px;
        border: 1px solid $border-color;
        color: $text-regular;
      }

      .swap-arrow { color: $primary; font-weight: 700; font-size: 16px; }
    }
  }
}

/* ── 互动区 ──────────────────── */
.interaction-bar {
  border-top: 1px solid $border-color;
  padding-top: 12px;
}

.stats-row {
  display: flex;
  gap: 18px;
  margin-bottom: 10px;

  .stat {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 13px;
    color: $text-secondary;
    .el-icon { font-size: 14px; }
  }
}

.action-row {
  display: flex;
  justify-content: space-around;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  flex: 1;
  justify-content: center;
  padding: 8px 0;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: $text-secondary;
  border-radius: 10px;
  transition: $transition-fast;
  letter-spacing: $letter-spacing-base;

  &:hover { background: $bg-subtle; color: $primary; }
  &.active { color: $primary; font-weight: 600; }
  &.fav.active { color: $warning; }

  .el-icon { font-size: 15px; }
}

/* ── 评论区 ──────────────────── */
.comment-area {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid $border-color;

  .el-textarea { margin-bottom: 10px; }

  .comment-actions {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 8px;

    .emoji-wrap {
      position: relative;
      margin-right: auto;

      .emoji-picker-popup {
        position: absolute;
        bottom: 32px;
        left: 0;
        z-index: 9999;
        box-shadow: $shadow-lg;
        border-radius: $border-radius;
        overflow: hidden;
      }
    }
  }
}

/* ── 响应式 ──────────────────── */
@media (max-width: 768px) {
  .square-card { padding: 16px; }
  .action-btn span { display: none; }
  .item-embed .embed-img { height: 140px; }
}
</style>