<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { Search, Refresh, Clock, Star, Plus, TrendCharts } from '@element-plus/icons-vue'
import { squareApi } from '@/api/modules/square'
import SquareCard from '@/components/square/SquareCard.vue'
import TagFilter from '@/components/square/TagFilter.vue'
import PublishPostDialog from '@/components/square/PublishPostDialog.vue'
import { usePagination } from '@/composables/usePagination'

// 状态管理
const keyword = ref('')
const selectedTags = ref([])
const activeTab = ref('recommend') // recommend | latest | hot
const stats = ref(null)
const showPublishDialog = ref(false)

// 分页 - 创建一个适配器函数给 usePagination
async function fetchPostsForPagination(params) {
  const { page, size, ...rest } = params
  const fetchParams = {
    page,
    size,
    sort: activeTab.value,
    keyword: keyword.value || undefined,
    tag: selectedTags.value.length > 0 ? selectedTags.value.join(',') : undefined,
    ...rest
  }
  
  // 调用真实API
  try {
    const response = await squareApi.getPosts(fetchParams)
    // API拦截器已经处理了错误，这里直接使用返回的数据
    return {
      records: response?.records || [],
      total: response?.total || 0
    }
  } catch (error) {
    console.error('获取动态列表异常:', error)
    return {
      records: [],
      total: 0
    }
  }
}

const { pagination, reset, fetchData, onPageChange, list: posts, loading: paginationLoading } = usePagination(fetchPostsForPagination, { size: 10 })
const page = computed(() => pagination.page)
const size = computed(() => pagination.size)
const total = computed(() => pagination.total)

// 加载更多
function loadMore() {
  onPageChange(page.value + 1)
}

// Tab选项
const tabOptions = [
  { value: 'recommend', label: '推荐', icon: Star },
  { value: 'latest', label: '最新', icon: Clock },
  { value: 'hot', label: '热门', icon: TrendCharts }
]

// 注意：fetchPosts 函数已被 fetchPostsForPagination 替代
// usePagination 会自动管理 posts 列表

// 获取统计数据
async function fetchStats() {
  try {
    // 调用真实API - API拦截器已经处理了错误，这里直接使用返回的数据
    const response = await squareApi.getStats()
    stats.value = response || {
      todayPosts: 24,
      todaySwaps: 8,
      totalPosts: 1248,
      totalUsers: 356
    }
  } catch (error) {
    console.error('获取统计数据异常:', error)
    // 使用模拟数据作为后备
    stats.value = {
      todayPosts: 24,
      todaySwaps: 8,
      totalPosts: 1248,
      totalUsers: 356
    }
  }
}

// 搜索
function handleSearch() {
  reset()
}

// 清空搜索
function clearSearch() {
  keyword.value = ''
  reset()
}

// 切换Tab
function handleTabChange(tab) {
  activeTab.value = tab
  reset()
}

// 刷新数据
function handleRefresh() {
  reset()
  fetchStats()
}

// 处理点赞变化
function handleLikeChanged({ postId, liked, count }) {
  const post = posts.value.find(p => p.id === postId)
  if (post) {
    post.likeCount = count
    post.isLiked = liked
  }
}

// 处理收藏变化
function handleFavoriteChanged({ postId, favorited, count }) {
  const post = posts.value.find(p => p.id === postId)
  if (post) {
    post.favoriteCount = count
    post.isFavorited = favorited
  }
}

// 处理评论添加
function handleCommentAdded({ postId }) {
  const post = posts.value.find(p => p.id === postId)
  if (post) {
    post.commentCount++
  }
}

// 处理动态删除
function handlePostDeleted(postId) {
  // 从列表中移除被删除的动态
  const index = posts.value.findIndex(p => p.id === postId)
  if (index !== -1) {
    posts.value.splice(index, 1)
    // 更新总数
    pagination.total = Math.max(0, pagination.total - 1)
  }
}

// 监听Tab变化
watch(activeTab, () => {
  reset()
})

// 监听标签变化
watch(selectedTags, () => {
  reset()
}, { deep: true })

onMounted(() => {
  fetchData()
  fetchStats()
})


// 处理发布动态
function handlePublishClick() {
  showPublishDialog.value = true
}

function handlePublishSuccess() {
  // 发布成功后刷新动态列表
  reset()

  // 如果有统计信息，也刷新统计
  if (stats.value) {
    fetchStats()
  }
}
</script>

<template>
  <div class="square-view">
    <!-- 页面标题和统计 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">换物广场</h1>
        <p class="page-subtitle">发现校园闲置好物，分享换物乐趣</p>
      </div>
      
      <div v-if="stats" class="stats-container">
        <div class="stat-item">
          <div class="stat-value">{{ stats.todayPosts }}</div>
          <div class="stat-label">今日动态</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.todaySwaps }}</div>
          <div class="stat-label">今日成交</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.totalPosts }}</div>
          <div class="stat-label">总动态</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.totalUsers }}</div>
          <div class="stat-label">活跃用户</div>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-section">
      <el-row justify="center" class="search-bar">
        <el-col :span="18" :md="16" :lg="14">
          <el-input
            v-model="keyword"
            placeholder="搜索动态、物品或用户..."
            size="large"
            clearable
            @keyup.enter="handleSearch"
            @clear="clearSearch"
          >
            <template #append>
              <el-button :icon="Search" @click="handleSearch" />
            </template>
          </el-input>
        </el-col>
        <el-col :span="4" :md="2" class="refresh-col">
          <el-button :icon="Refresh" @click="handleRefresh" title="刷新" />
        </el-col>
      </el-row>
    </div>

    <!-- Tab切换 -->
    <div class="tab-section">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="tab in tabOptions"
          :key="tab.value"
          :name="tab.value"
        >
          <template #label>
            <span class="tab-label">
              <el-icon><component :is="tab.icon" /></el-icon>
              {{ tab.label }}
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 标签筛选 -->
    <TagFilter v-model="selectedTags" />

    <!-- 动态列表 -->
    <div class="posts-section">
      <div v-loading="paginationLoading && page === 1" class="posts-container">
        <template v-if="posts.length > 0">
          <SquareCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            @like-changed="handleLikeChanged"
            @favorite-changed="handleFavoriteChanged"
            @comment-added="handleCommentAdded"
            @post-deleted="handlePostDeleted"
          />
        </template>
        
        <div v-else-if="!paginationLoading" class="empty-state">
          <el-empty description="暂无动态" />
          <p class="empty-hint">尝试发布第一个动态或调整筛选条件</p>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="posts.length > 0 && posts.length < total" class="load-more">
        <el-button
          :loading="paginationLoading"
          type="primary"
          link
          @click="loadMore"
          class="load-more-btn"
        >
          {{ paginationLoading ? '加载中...' : '加载更多' }}
        </el-button>
      </div>
      
      <!-- 没有更多数据 -->
      <div v-if="posts.length > 0 && posts.length >= total" class="no-more">
        <el-divider>
          <span class="no-more-text">没有更多动态了</span>
        </el-divider>
      </div>
    </div>

    <!-- 发布动态按钮（右下角浮动） -->
    <div class="publish-fab">
      <el-button
        type="primary"
        size="large"
        round
        class="publish-btn"
        @click="handlePublishClick"
      >
        <el-icon><Plus /></el-icon>
        发布动态
      </el-button>
    </div>

    <!-- 发布动态对话框 -->
    <PublishPostDialog
      :visible="showPublishDialog"
      @update:visible="showPublishDialog = $event"
      @success="handlePublishSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.square-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 0 40px;
}

.page-header {
  text-align: center;
  margin-bottom: 28px;
  padding: 36px 20px 32px;
  background: linear-gradient(135deg, rgba(27,153,170,0.06) 0%, rgba(158,208,204,0.1) 100%);
  border-radius: $border-radius-lg;

  .header-content {
    margin-bottom: 24px;

    .page-title {
      font-size: 32px;
      font-weight: 700;
      margin-bottom: 8px;
      background: linear-gradient(135deg, $primary, $primary-light);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      letter-spacing: $letter-spacing-wide;
    }

    .page-subtitle {
      font-size: 15px;
      color: $text-secondary;
      letter-spacing: $letter-spacing-base;
    }
  }

  .stats-container {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
    gap: 20px;

    .stat-item {
      text-align: center;
      min-width: 90px;
      padding: 12px 16px;
      background: rgba(255,255,255,0.7);
      border-radius: $border-radius-sm;
      backdrop-filter: blur(4px);

      .stat-value {
        font-size: 24px;
        font-weight: 700;
        color: $primary;
        margin-bottom: 3px;
        letter-spacing: 0.02em;
      }

      .stat-label {
        font-size: 12px;
        color: $text-secondary;
        letter-spacing: $letter-spacing-base;
      }
    }
  }
}

.search-section {
  margin-bottom: 20px;

  .search-bar {
    .refresh-col {
      display: flex;
      justify-content: flex-end;
      align-items: center;
    }
  }
}

.tab-section {
  margin-bottom: 20px;
  background: $bg-card;
  border-radius: $border-radius;
  padding: 4px 16px 0;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;

  .tab-label {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 500;
    font-size: 14px;
  }

  :deep(.el-tabs__nav-wrap::after) { height: 1px; }
  :deep(.el-tabs__item) { font-size: 14px; padding: 0 18px; }
}

.posts-section {
  .posts-container { min-height: 300px; }

  .empty-state {
    text-align: center;
    padding: 60px 0;

    .empty-hint { margin-top: 12px; color: $text-secondary; font-size: 13px; }
  }

  .load-more {
    text-align: center;
    padding: 20px 0;

    .load-more-btn { font-size: 14px; font-weight: 500; }
  }

  .no-more {
    padding: 20px 0;
    .no-more-text { color: $text-secondary; font-size: 13px; }
  }
}

.publish-fab {
  position: fixed;
  right: 36px;
  bottom: 36px;
  z-index: 1000;

  .publish-btn {
    padding: 12px 24px;
    font-size: 15px;
    font-weight: 500;
    box-shadow: $shadow-lg;
    letter-spacing: $letter-spacing-base;

    .el-icon { margin-right: 6px; }
  }
}

@media (max-width: 768px) {
  .square-view { padding: 0 0 28px; }
  .page-header {
    padding: 24px 16px 20px;
    .header-content .page-title { font-size: 24px; }
    .stats-container { gap: 10px; .stat-item { min-width: 72px; .stat-value { font-size: 18px; } } }
  }
  .publish-fab { right: 18px; bottom: 18px; .publish-btn { padding: 10px 18px; font-size: 13px; } }
}
</style>