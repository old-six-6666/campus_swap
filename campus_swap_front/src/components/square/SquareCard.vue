<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Star,
  ChatDotRound,
  More,
  User,
  Picture,
  CircleCheck,
  Delete,
  Edit,
  Warning
} from '@element-plus/icons-vue'
import EmojiPicker from 'vue3-emoji-picker'
import 'vue3-emoji-picker/css'
import { squareApi } from '@/api/modules/square'
import { useUserStore } from '@/stores/useUserStore'

const props = defineProps({
  post: { type: Object, required: true }
})

const emit = defineEmits(['like-changed', 'favorite-changed', 'comment-added', 'post-deleted', 'post-updated'])

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

function handleAction(command) {
  if (command === 'edit') {
    router.push({ name: 'EditPost', params: { id: props.post.id } })
  } else if (command === 'delete') {
    handleDelete()
  } else if (command === 'report') {
    openReportDialog()
  }
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

// 检查当前用户是否是动态的作者
const isCurrentUserPost = computed(() => {
  return userStore.isLoggedIn && userStore.userInfo?.id === props.post.userId
})

// 封面图优先级：动态第一张图 > 物品封面图
const coverSrc = computed(() => {
  if (props.post.images?.length) return props.post.images[0]
  if (props.post.item?.coverImage) return props.post.item.coverImage
  return null
})

// ===== 举报 =====
const showReportDialog = ref(false)
const reportForm = ref({ reason: null, description: '' })
const submittingReport = ref(false)

const reportReasons = [
  { value: 1, label: '违法违规' },
  { value: 2, label: '色情低俗' },
  { value: 3, label: '虚假信息' },
  { value: 4, label: '侮辱谩骂' },
  { value: 5, label: '广告骚扰' },
  { value: 6, label: '其他' },
]

function openReportDialog() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  reportForm.value = { reason: null, description: '' }
  showReportDialog.value = true
}

async function submitReport() {
  if (!reportForm.value.reason) {
    ElMessage.warning('请选择举报原因')
    return
  }
  submittingReport.value = true
  try {
    await squareApi.reportPost(props.post.id, {
      reason: reportForm.value.reason,
      description: reportForm.value.description
    })
    showReportDialog.value = false
    ElMessage.success('举报已提交，我们将尽快审核')
  } catch (error) {
    const msg = error?.response?.data?.message
    ElMessage.error(msg || '举报失败，请稍后再试')
  } finally {
    submittingReport.value = false
  }
}
</script>

<template>
  <div class="card-wrapper">
  <!-- 小红书风格卡片：点击跳详情，互动按钮阻止冒泡 -->
  <div class="square-card" @click="router.push({ name: 'PostDetail', params: { id: post.id } })">

    <!-- 封面图：有图才显示 -->
    <div v-if="coverSrc" class="card-cover">
      <el-image :src="coverSrc" fit="cover" class="cover-img">
        <template #error>
          <div class="cover-placeholder"><el-icon><Picture /></el-icon></div>
        </template>
      </el-image>

      <!-- 多图角标 -->
      <span v-if="post.images?.length > 1" class="img-count">{{ post.images.length }}图</span>
      <!-- 物品价格角标 -->
      <span v-if="post.type === 1 && post.item?.price != null" class="price-badge">¥{{ post.item.price }}</span>
    </div>

    <!-- 右上角操作菜单（始终可见，移出封面区） -->
    <div class="card-menu" @click.stop>
      <el-dropdown v-if="isCurrentUserPost" trigger="click" @command="handleAction">
        <el-button class="menu-btn" text><el-icon><More /></el-icon></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="edit"><el-icon><Edit /></el-icon>编辑</el-dropdown-item>
            <el-dropdown-item command="delete" style="color:#f56c6c"><el-icon><Delete /></el-icon>删除</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-dropdown v-else-if="userStore.isLoggedIn" trigger="click" @command="handleAction">
        <el-button class="menu-btn" text><el-icon><More /></el-icon></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="report" style="color:#e6a23c"><el-icon><Warning /></el-icon>举报</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 卡片内容区 -->
    <div class="card-body">
      <!-- 正文（最多3行） -->
      <p class="card-content">{{ post.content || post.item?.title || '动态' }}</p>

      <!-- 标签 -->
      <div v-if="post.tags?.length" class="card-tags">
        <span v-for="tag in post.tags.slice(0, 3)" :key="tag.id" class="tag">#{{ tag.name }}</span>
      </div>

      <!-- 用户信息 + 互动 -->
      <div class="card-footer" @click.stop>
        <div class="author" @click="router.push({ name: 'UserHome', params: { id: post.userId } })">
          <el-avatar :size="22" :src="post.user?.avatar" class="author-avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <span class="author-name">{{ post.user?.username || '匿名' }}</span>
        </div>

        <div class="footer-actions">
          <!-- 点赞 -->
          <button class="action-btn" :class="{ active: isLiked }" @click="handleLike">
            <el-icon><CircleCheck /></el-icon>
            <span>{{ likeCount }}</span>
          </button>
          <!-- 收藏 -->
          <button class="action-btn fav" :class="{ active: isFavorited }" @click="handleFavorite">
            <el-icon><Star /></el-icon>
            <span>{{ favoriteCount }}</span>
          </button>
          <!-- 评论 -->
          <button class="action-btn" @click="showCommentInput = !showCommentInput">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ commentCount }}</span>
          </button>
        </div>
      </div>

      <!-- 评论输入框 -->
      <div v-if="showCommentInput" class="comment-input-area" @click.stop>
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
            <el-button type="info" link size="small" @click="showCommentEmoji = !showCommentEmoji">😊</el-button>
            <EmojiPicker
              v-if="showCommentEmoji"
              class="emoji-picker-popup"
              :native="true"
              :disable-skin-tones="true"
              @select="onSelectEmoji"
            />
          </div>
          <el-button size="small" @click="showCommentInput = false; showCommentEmoji = false">取消</el-button>
          <el-button type="primary" size="small" :loading="postingComment" @click="handleComment">发布</el-button>
        </div>
      </div>
    </div>
  </div>

  <!-- 举报对话框 -->
  <el-dialog v-model="showReportDialog" title="举报动态" width="420px" append-to-body>
    <el-form label-width="80px">
      <el-form-item label="举报原因" required>
        <el-radio-group v-model="reportForm.reason" class="report-reason-group">
          <el-radio v-for="r in reportReasons" :key="r.value" :value="r.value">{{ r.label }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="补充说明">
        <el-input v-model="reportForm.description" type="textarea" :rows="3"
          placeholder="请描述具体情况（选填，最多200字）" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showReportDialog = false">取消</el-button>
      <el-button type="warning" :loading="submittingReport" @click="submitReport">提交举报</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<style scoped lang="scss">
// 透明包裹层，让 columns 瀑布流的 break-inside 作用在 .square-card 上
.card-wrapper {
  break-inside: avoid;
  margin-bottom: 12px;
}

.square-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  position: relative;
  transition: box-shadow 0.2s, transform 0.2s;

  &:hover {
    box-shadow: 0 4px 18px rgba(0, 0, 0, 0.14);
    transform: translateY(-2px);
  }
}

/* ---- 封面图 ---- */
.card-cover {
  position: relative;
  width: 100%;
  min-height: 120px;
  background: #f5f7fa;

  .cover-img {
    width: 100%;
    display: block;
    max-height: 260px;
    :deep(img) {
      width: 100%;
      height: auto;
      max-height: 260px;
      object-fit: cover;
      display: block;
    }
  }

  .cover-placeholder {
    width: 100%;
    height: 140px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #c0c4cc;
    background: #f5f7fa;
  }

  .img-count {
    position: absolute;
    top: 8px;
    left: 8px;
    background: rgba(0,0,0,0.45);
    color: #fff;
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 10px;
  }

  .price-badge {
    position: absolute;
    bottom: 8px;
    left: 8px;
    background: rgba(245, 108, 108, 0.9);
    color: #fff;
    font-size: 12px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 10px;
  }
}

/* ---- 操作菜单（绝对定位到卡片右上角） ---- */
.card-menu {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 1;

  .menu-btn {
    width: 26px;
    height: 26px;
    background: rgba(0,0,0,0.18);
    color: #606266;
    border-radius: 50%;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;

    &:hover {
      background: rgba(0,0,0,0.32);
      color: #303133;
    }
  }
}

/* ---- 卡片内容 ---- */
.card-body {
  padding: 10px 12px 8px;
}

.card-content {
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
  margin: 0 0 6px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;

  .tag {
    font-size: 11px;
    color: #409eff;
  }
}

/* ---- 底栏：作者 + 互动 ---- */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;

  .author {
    display: flex;
    align-items: center;
    gap: 5px;
    cursor: pointer;
    min-width: 0;

    &:hover .author-name { color: #409eff; }

    .author-avatar { flex-shrink: 0; }

    .author-name {
      font-size: 12px;
      color: #909399;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 70px;
    }
  }

  .footer-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  font-size: 12px;
  color: #909399;
  transition: color 0.15s;

  &:hover { color: #606266; }
  &.active { color: #409eff; }
  &.fav.active { color: #e6a23c; }

  .el-icon { font-size: 14px; }
}

/* ---- 评论框 ---- */
.comment-input-area {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;

  .comment-actions {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 6px;
    margin-top: 8px;

    .emoji-wrap {
      position: relative;
      margin-right: auto;

      .emoji-picker-popup {
        position: absolute;
        bottom: 32px;
        left: 0;
        z-index: 9999;
        box-shadow: 0 4px 16px rgba(0,0,0,0.12);
        border-radius: 8px;
        overflow: hidden;
      }
    }
  }
}

.report-reason-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
