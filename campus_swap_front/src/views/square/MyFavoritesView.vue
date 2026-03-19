<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { squareApi } from '@/api/modules/square'
import { Star, Picture, User } from '@element-plus/icons-vue'

const router = useRouter()

const posts = ref([])
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(false)
const loadingMore = ref(false)

const postTypeMap = {
  1: { text: '发布了一个物品', icon: '📦' },
  2: { text: '换物成功', icon: '🎉' },
  3: { text: '分享动态', icon: '💬' }
}

function formatTime(time) {
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

async function fetchFavorites(isLoadMore = false) {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const res = await squareApi.getMyFavorites({ page: page.value, size })
    total.value = res.total || 0
    if (isLoadMore) {
      posts.value.push(...(res.records || []))
    } else {
      posts.value = res.records || []
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  page.value++
  fetchFavorites(true)
}

onMounted(() => fetchFavorites())
</script>

<template>
  <div class="my-favorites-view">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="title-icon"><Star /></el-icon>
        我的收藏
      </h2>
      <span class="total-tip">共 {{ total }} 条</span>
    </div>

    <div v-loading="loading" class="posts-container">
      <template v-if="posts.length > 0">
        <div
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          @click="router.push({ name: 'PostDetail', params: { id: post.id } })"
        >
          <!-- 封面图 -->
          <div class="post-cover">
            <el-image
              v-if="post.images && post.images.length > 0"
              :src="post.images[0]"
              fit="cover"
              class="cover-img"
            >
              <template #error>
                <div class="cover-placeholder"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="cover-placeholder">
              <span class="type-icon">{{ postTypeMap[post.type]?.icon || '📝' }}</span>
            </div>
          </div>

          <!-- 内容 -->
          <div class="post-body">
            <p class="post-content">{{ post.content }}</p>

            <div class="post-footer">
              <div class="post-user">
                <el-avatar :size="22" :src="post.user?.avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="username">{{ post.user?.username || '匿名' }}</span>
              </div>
              <div class="post-stats">
                <el-icon><Star /></el-icon>
                <span>{{ post.favoriteCount }}</span>
              </div>
            </div>

            <div class="favorite-time">收藏于 {{ formatTime(post.favoritedAt) }}</div>
          </div>
        </div>
      </template>

      <div v-else-if="!loading" class="empty-state">
        <el-empty description="还没有收藏任何动态">
          <el-button type="primary" @click="router.push({ name: 'Square' })">去广场逛逛</el-button>
        </el-empty>
      </div>
    </div>

    <!-- 加载更多 -->
    <div v-if="posts.length > 0 && posts.length < total" class="load-more">
      <el-button :loading="loadingMore" type="primary" link @click="loadMore">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </el-button>
    </div>
    <div v-if="posts.length > 0 && posts.length >= total && total > 0" class="no-more">
      <el-divider><span class="no-more-text">没有更多了</span></el-divider>
    </div>
  </div>
</template>

<style scoped lang="scss">
.my-favorites-view {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;

  .page-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 22px;
    font-weight: 700;
    color: #303133;
    margin: 0;

    .title-icon {
      font-size: 22px;
      color: #e6a23c;
    }
  }

  .total-tip {
    font-size: 14px;
    color: #909399;
    margin-left: 4px;
  }
}

.posts-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.post-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07);
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.13);
  }

  .post-cover {
    height: 150px;
    background: #f5f7fa;
    overflow: hidden;

    .cover-img {
      width: 100%;
      height: 100%;
    }

    .cover-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #c0c4cc;
      font-size: 36px;
    }
  }

  .post-body {
    padding: 12px 14px;

    .post-content {
      font-size: 14px;
      color: #303133;
      line-height: 1.5;
      margin: 0 0 10px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .post-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 6px;

      .post-user {
        display: flex;
        align-items: center;
        gap: 6px;

        .username {
          font-size: 13px;
          color: #606266;
        }
      }

      .post-stats {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: #e6a23c;
      }
    }

    .favorite-time {
      font-size: 12px;
      color: #c0c4cc;
    }
  }
}

.load-more,
.no-more {
  text-align: center;
  padding: 24px 0 0;

  .no-more-text {
    font-size: 13px;
    color: #c0c4cc;
  }
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

@media (max-width: 600px) {
  .posts-container {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
