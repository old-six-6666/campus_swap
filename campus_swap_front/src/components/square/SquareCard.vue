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
  post: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['like-changed', 'favorite-changed', 'comment-added'])

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

// 动态类型映射
const postTypeMap = {
  1: { text: '发布了一个物品', color: 'primary', icon: '📦' },
  2: { text: '换物成功', color: 'success', icon: '🎉' },
  3: { text: '分享动态', color: 'info', icon: '💬' }
}

// 获取动态类型信息
const postTypeInfo = computed(() => {
  return postTypeMap[props.post.type] || { text: '动态', color: 'default', icon: '📝' }
})

// 格式化时间
const formatTime = (time) => {
  const now = new Date()
  const postTime = new Date(time)
  const diff = now - postTime
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (minutes < 60) {
    return `${minutes}分钟前`
  } else if (hours < 24) {
    return `${hours}小时前`
  } else if (days < 30) {
    return `${days}天前`
  } else {
    return postTime.toLocaleDateString()
  }
}

// 点赞/取消点赞
async function handleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  // 乐观更新：先切换 UI
  const prevLiked = isLiked.value
  const prevCount = likeCount.value
  isLiked.value = !isLiked.value
  likeCount.value += isLiked.value ? 1 : -1

  try {
    if (isLiked.value) {
      await squareApi.likePost(props.post.id, { silent: true })
    } else {
      await squareApi.unlikePost(props.post.id, { silent: true })
    }
    emit('like-changed', { postId: props.post.id, liked: isLiked.value, count: likeCount.value })
  } catch {
    // 请求失败则回滚
    isLiked.value = prevLiked
    likeCount.value = prevCount
  }
}

// 收藏/取消收藏
async function handleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  // 乐观更新：先切换 UI
  const prevFavorited = isFavorited.value
  const prevCount = favoriteCount.value
  isFavorited.value = !isFavorited.value
  favoriteCount.value += isFavorited.value ? 1 : -1

  try {
    if (isFavorited.value) {
      await squareApi.favoritePost(props.post.id, { silent: true })
    } else {
      await squareApi.unfavoritePost(props.post.id, { silent: true })
    }
    emit('favorite-changed', { postId: props.post.id, favorited: isFavorited.value, count: favoriteCount.value })
  } catch {
    // 请求失败则回滚
    isFavorited.value = prevFavorited
    favoriteCount.value = prevCount
  }
}

// 发表评论
async function handleComment() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  if (!commentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  
  postingComment.value = true
  try {
    await squareApi.addComment({
      postId: props.post.id,
      content: commentContent.value.trim()
    })
    
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

// 分享动态
function handleShare() {
  const shareUrl = `${window.location.origin}/square/post/${props.post.id}`
  navigator.clipboard.writeText(shareUrl)
    .then(() => {
      ElMessage.success('链接已复制到剪贴板')
    })
    .catch(() => {
      ElMessage.info(`分享链接: ${shareUrl}`)
    })
  // 分享数 +1（静默，不影响 UI）
  squareApi.sharePost(props.post.id).catch(() => {})
}

// 处理下拉菜单操作
function handleAction(command) {
  if (command === 'delete') {
    handleDelete()
  }
}

// 删除动态
async function handleDelete() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      '确定要删除这条动态吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
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

// 检查当前用户是否是动态的作者
const isCurrentUserPost = computed(() => {
  return userStore.isLoggedIn && userStore.userInfo?.id === props.post.userId
})
</script>

<template>
  <div class="square-card">
    <!-- 用户信息栏 -->
    <div class="user-info">
      <div class="user-avatar clickable-user" @click.stop="router.push({ name: 'UserHome', params: { id: post.userId } })">
        <el-avatar :size="40" :src="post.user?.avatar">
          <el-icon><User /></el-icon>
        </el-avatar>
      </div>
      <div class="user-details">
        <div class="username clickable-user" @click.stop="router.push({ name: 'UserHome', params: { id: post.userId } })">{{ post.user?.username || '匿名用户' }}</div>
        <div class="post-meta">
          <span class="post-type">
            <span class="type-icon">{{ postTypeInfo.icon }}</span>
            {{ postTypeInfo.text }}
          </span>
          <span class="post-time">· {{ formatTime(post.createdAt) }}</span>
        </div>
      </div>
      <div class="post-actions">
        <el-dropdown v-if="isCurrentUserPost" trigger="click" @command="handleAction">
          <el-button type="info" link :icon="More" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="delete" style="color: #f56c6c;">
                <el-icon><Delete /></el-icon>
                删除动态
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 动态内容 -->
    <div class="post-content" @click="router.push({ name: 'PostDetail', params: { id: post.id } })" style="cursor: pointer;">
      <p v-if="post.content" class="content-text">{{ post.content }}</p>

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
          @click.stop
        >
          <template #error>
            <div class="post-image-error"><el-icon><Picture /></el-icon></div>
          </template>
        </el-image>
      </div>
      
      <!-- 物品信息（如果是发布物品类型） -->
      <div v-if="post.type === 1 && post.item" class="item-info">
        <div class="item-images">
          <el-image
            v-if="post.item.coverImage"
            :src="post.item.coverImage"
            fit="cover"
            class="item-image"
            :preview-src-list="[post.item.coverImage]"
          >
            <template #error>
              <div class="image-placeholder">
                <el-icon size="24"><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </div>
        <div class="item-details">
          <h4 class="item-title">{{ post.item.title }}</h4>
          <div class="item-meta">
            <el-tag size="small" type="info">{{ post.item.condition || '未知成色' }}</el-tag>
            <span class="item-price">¥ {{ post.item.price }}</span>
          </div>
          <p v-if="post.item.description" class="item-desc">{{ post.item.description }}</p>
          
          <!-- 标签 -->
          <div v-if="post.tags && post.tags.length > 0" class="item-tags">
            <el-tag
              v-for="tag in post.tags"
              :key="tag.id"
              size="small"
              class="tag-item"
            >
              #{{ tag.name }}
            </el-tag>
          </div>
          
          <!-- 期望交换 -->
          <div v-if="post.item.exchangeFor" class="exchange-for">
            <span class="exchange-label">想换：</span>
            <span class="exchange-content">{{ post.item.exchangeFor }}</span>
          </div>
        </div>
      </div>

      <!-- 换物成功信息（类型2） -->
      <div v-if="post.type === 2 && post.swapRecord" class="swap-success">
        <div class="swap-icon">🎉</div>
        <div class="swap-content">
          <p>成功与 <strong>{{ post.swapRecord.partnerName }}</strong> 完成换物</p>
          <div class="swap-items">
            <span class="item-a">{{ post.swapRecord.itemATitle }}</span>
            <span class="swap-arrow">↔</span>
            <span class="item-b">{{ post.swapRecord.itemBTitle }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 互动区域 -->
    <div class="interaction-area">
      <div class="interaction-stats">
        <span class="stat-item">
          <el-icon><CircleCheck /></el-icon>
          {{ likeCount }}
        </span>
        <span class="stat-item">
          <el-icon><Star /></el-icon>
          {{ favoriteCount }}
        </span>
        <span class="stat-item">
          <el-icon><ChatDotRound /></el-icon>
          {{ commentCount }}
        </span>
      </div>
      
      <div class="interaction-buttons">
        <el-button
          type="primary"
          link
          :class="{ 'is-active': isLiked }"
          :icon="CircleCheck"
          @click="handleLike"
          class="interaction-btn"
        >
          {{ isLiked ? '已赞' : '点赞' }}
        </el-button>

        <el-button
          type="warning"
          link
          :class="{ 'is-active': isFavorited }"
          :icon="Star"
          @click="handleFavorite"
          class="interaction-btn"
        >
          {{ isFavorited ? '已收藏' : '收藏' }}
        </el-button>

        <el-button
          type="info"
          link
          :icon="ChatDotRound"
          @click="showCommentInput = !showCommentInput"
          class="interaction-btn"
        >
          评论
        </el-button>

        <el-button
          type="info"
          link
          :icon="Share"
          @click="handleShare"
          class="interaction-btn"
        >
          分享
        </el-button>
      </div>
    </div>

    <!-- 评论输入框 -->
    <div v-if="showCommentInput" class="comment-input-area">
      <el-input
        v-model="commentContent"
        type="textarea"
        :rows="2"
        placeholder="写下你的评论..."
        maxlength="500"
        show-word-limit
        class="comment-input"
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
        <el-button
          type="primary"
          size="small"
          :loading="postingComment"
          @click="handleComment"
        >
          发表评论
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.square-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  }
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  
  .user-avatar {
    margin-right: 12px;
  }

  .clickable-user {
    cursor: pointer;
    &:hover {
      opacity: 0.8;
    }
  }

  .user-details {
    flex: 1;

    .username {
      font-weight: 600;
      font-size: 16px;
      color: #303133;
      margin-bottom: 4px;
      display: inline-block;
      &:hover {
        color: #409eff;
      }
    }
    
    .post-meta {
      font-size: 13px;
      color: #909399;
      
      .post-type {
        color: #409eff;
        font-weight: 500;
        
        .type-icon {
          margin-right: 4px;
        }
      }
      
      .post-time {
        margin-left: 8px;
      }
    }
  }
  
  .post-actions {
    margin-left: auto;
  }
}

.post-content {
  margin-bottom: 16px;
  
  .content-text {
    font-size: 15px;
    line-height: 1.6;
    color: #303133;
    margin-bottom: 12px;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .post-images {
    display: grid;
    gap: 4px;
    margin-bottom: 12px;

    &.count-1 {
      grid-template-columns: 1fr;
      .post-image { height: 200px; border-radius: 8px; }
    }
    &.count-2, &.count-4 {
      grid-template-columns: repeat(2, 1fr);
      .post-image { height: 140px; border-radius: 6px; }
    }
    &.count-3, &.count-5, &.count-6,
    &.count-7, &.count-8, &.count-9 {
      grid-template-columns: repeat(3, 1fr);
      .post-image { height: 110px; border-radius: 6px; }
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
      background: #f5f7fa;
      color: #c0c4cc;
    }
  }
  
  .item-info {
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    padding: 16px;
    background: #f8f9fa;
    
    .item-images {
      margin-bottom: 12px;
      
      .item-image {
        width: 100%;
        height: 180px;
        border-radius: 6px;
        object-fit: cover;
      }
      
      .image-placeholder {
        width: 100%;
        height: 180px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f5f7fa;
        color: #c0c4cc;
        border-radius: 6px;
      }
    }
    
    .item-details {
      .item-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        margin: 0 0 8px;
      }
      
      .item-meta {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
        
        .item-price {
          font-size: 18px;
          font-weight: 700;
          color: #f56c6c;
        }
      }
      
      .item-desc {
        font-size: 14px;
        color: #606266;
        line-height: 1.5;
        margin-bottom: 12px;
      }
      
      .item-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin-bottom: 12px;
        
        .tag-item {
          background: #f0f9ff;
          border-color: #d9ecff;
          color: #409eff;
        }
      }
      
      .exchange-for {
        padding: 8px 12px;
        background: #fff7e6;
        border-radius: 6px;
        border-left: 4px solid #ffc107;
        
        .exchange-label {
          font-weight: 600;
          color: #e6a23c;
        }
        
        .exchange-content {
          color: #606266;
        }
      }
    }
  }
  
  .swap-success {
    display: flex;
    align-items: center;
    padding: 16px;
    background: linear-gradient(135deg, #f0f9ff, #e6f7ff);
    border-radius: 8px;
    border: 1px solid #d9ecff;
    
    .swap-icon {
      font-size: 32px;
      margin-right: 16px;
    }
    
    .swap-content {
      flex: 1;
      
      p {
        margin: 0 0 8px;
        color: #303133;
        font-size: 15px;
      }
      
      .swap-items {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 14px;
        
        .item-a, .item-b {
          padding: 4px 12px;
          background: #fff;
          border-radius: 4px;
          border: 1px solid #e4e7ed;
        }
        
        .swap-arrow {
          color: #409eff;
          font-weight: bold;
        }
      }
    }
  }
}

.interaction-area {
  border-top: 1px solid #f0f2f5;
  padding-top: 16px;
  
  .interaction-stats {
    display: flex;
    gap: 20px;
    margin-bottom: 12px;
    
    .stat-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: #909399;
      
      .el-icon {
        font-size: 16px;
      }
    }
  }
  
  .interaction-buttons {
    display: flex;
    justify-content: space-around;

    .interaction-btn {
      flex: 1;
      padding: 8px 0;
      color: #909399;

      &.is-active {
        font-weight: 600;
      }

      // 点赞激活：蓝色
      &.el-button--primary.is-active {
        color: #409eff;
      }

      // 收藏激活：橙色
      &.el-button--warning.is-active {
        color: #e6a23c;
      }
    }
  }
}

.comment-input-area {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;

  .comment-input {
    margin-bottom: 12px;
  }

  .comment-actions {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 8px;

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
  }
}

// 响应式调整
@media (max-width: 768px) {
  .square-card {
    padding: 16px;
    margin-bottom: 12px;
  }
  
  .interaction-buttons {
    .interaction-btn {
      span {
        display: none;
      }
    }
  }
  
  .item-info {
    .item-images .item-image {
      height: 140px !important;
    }
  }
}
</style>