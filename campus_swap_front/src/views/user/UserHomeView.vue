<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Picture, ChatDotRound, User as UserIcon } from '@element-plus/icons-vue'
import { userApi } from '@/api/modules/user'
import { itemApi } from '@/api/modules/item'
import { squareApi } from '@/api/modules/square'
import { useUserStore } from '@/stores/useUserStore'
import SquareCard from '@/components/square/SquareCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userInfo = ref(null)
const userLoading = ref(false)

// 商品 Tab
const items = ref([])
const itemsLoading = ref(false)
const itemsPage = ref(1)
const itemsTotal = ref(0)
const itemsSize = 12

// 动态 Tab
const posts = ref([])
const postsLoading = ref(false)
const postsPage = ref(1)
const postsTotal = ref(0)
const postsSize = 10

const activeTab = ref('items')

const targetUserId = computed(() => Number(route.params.id))
const isSelf = computed(() => userStore.userInfo?.id === targetUserId.value)

async function fetchUser() {
  userLoading.value = true
  try {
    userInfo.value = await userApi.getUserProfile(targetUserId.value)
  } catch (e) {
    console.error('[UserHome] fetchUser failed, userId=', targetUserId.value, e)
    userInfo.value = null
  } finally {
    userLoading.value = false
  }
}

async function fetchItems(page = 1) {
  itemsLoading.value = true
  try {
    const data = await itemApi.getUserItems(targetUserId.value, { page, size: itemsSize })
    items.value = data.records || []
    itemsTotal.value = data.total || 0
    itemsPage.value = page
  } catch {
    items.value = []
  } finally {
    itemsLoading.value = false
  }
}

async function fetchPosts(page = 1) {
  postsLoading.value = true
  try {
    const data = await squareApi.getUserPosts(targetUserId.value, { page, size: postsSize })
    posts.value = data.records || []
    postsTotal.value = data.total || 0
    postsPage.value = page
  } catch {
    posts.value = []
  } finally {
    postsLoading.value = false
  }
}

function handleTabChange(tab) {
  if (tab === 'posts' && posts.value.length === 0) {
    fetchPosts(1)
  }
}

function contactUser() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  router.push({
    name: 'Chat',
    query: {
      to: userInfo.value.id,
      nickname: userInfo.value.nickname,
      avatar: userInfo.value.avatar || '',
    },
  })
}

onMounted(async () => {
  await fetchUser()
  await fetchItems(1)
})

// 当路由参数变化时（从一个用户主页跳转到另一个），重新加载数据
watch(targetUserId, () => {
  userInfo.value = null
  items.value = []
  posts.value = []
  activeTab.value = 'items'
  fetchUser()
  fetchItems(1)
})
</script>

<template>
  <div class="user-home" v-loading="userLoading">
    <el-empty v-if="!userLoading && !userInfo" description="用户不存在" />

    <template v-if="userInfo">
      <!-- 用户信息卡片 -->
      <el-card class="profile-card" shadow="never">
        <div class="profile-header">
          <el-avatar :size="80" :src="userInfo.avatar" class="profile-avatar">
            <el-icon :size="40"><UserIcon /></el-icon>
          </el-avatar>
          <div class="profile-info">
            <div class="nickname-row">
              <span class="nickname">{{ userInfo.nickname }}</span>
              <el-tag v-if="userInfo.isVerified === 1" type="success" size="small" effect="plain">
                已认证
              </el-tag>
            </div>
            <div v-if="userInfo.school" class="school">{{ userInfo.school }}</div>
          </div>
          <div class="profile-actions">
            <el-button
              v-if="!isSelf && userStore.isLoggedIn"
              type="primary"
              :icon="ChatDotRound"
              @click="contactUser"
            >
              私信
            </el-button>
            <el-button
              v-if="isSelf"
              type="default"
              @click="router.push({ name: 'Profile' })"
            >
              编辑资料
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- Tab 切换 -->
      <el-tabs v-model="activeTab" class="content-tabs" @tab-change="handleTabChange">
        <!-- 在售物品 -->
        <el-tab-pane label="在售物品" name="items">
          <div v-loading="itemsLoading" class="items-grid">
            <el-empty v-if="!itemsLoading && items.length === 0" description="暂无在售物品" :image-size="80" />
            <el-row :gutter="16">
              <el-col
                v-for="item in items"
                :key="item.id"
                :xs="12" :sm="8" :md="6"
              >
                <RouterLink :to="`/item/${item.id}`" class="item-card-link">
                  <el-card shadow="hover" class="item-card">
                    <el-image
                      :src="item.coverImage"
                      fit="cover"
                      class="item-image"
                      lazy
                    >
                      <template #error>
                        <div class="image-placeholder">
                          <el-icon size="28"><Picture /></el-icon>
                        </div>
                      </template>
                    </el-image>
                    <div class="item-info">
                      <p class="item-title">{{ item.title }}</p>
                      <p class="item-price">¥ {{ item.price }}</p>
                    </div>
                  </el-card>
                </RouterLink>
              </el-col>
            </el-row>
            <div v-if="itemsTotal > itemsSize" class="pagination-wrap">
              <el-pagination
                :current-page="itemsPage"
                :page-size="itemsSize"
                :total="itemsTotal"
                layout="prev, pager, next"
                @current-change="fetchItems"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 发布动态 -->
        <el-tab-pane label="发布动态" name="posts">
          <div v-loading="postsLoading" class="posts-list">
            <el-empty v-if="!postsLoading && posts.length === 0" description="暂无动态" :image-size="80" />
            <SquareCard
              v-for="post in posts"
              :key="post.id"
              :post="post"
            />
            <div v-if="postsTotal > postsSize" class="pagination-wrap">
              <el-pagination
                :current-page="postsPage"
                :page-size="postsSize"
                :total="postsTotal"
                layout="prev, pager, next"
                @current-change="fetchPosts"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<style scoped lang="scss">
.user-home {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-card {
  border-radius: 12px;

  .profile-header {
    display: flex;
    align-items: center;
    gap: 20px;

    .profile-avatar {
      flex-shrink: 0;
    }

    .profile-info {
      flex: 1;

      .nickname-row {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 6px;

        .nickname {
          font-size: 20px;
          font-weight: 700;
          color: #303133;
        }
      }

      .school {
        font-size: 14px;
        color: #909399;
      }
    }

    .profile-actions {
      flex-shrink: 0;
    }
  }
}

.content-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }
}

.items-grid {
  .item-card-link {
    text-decoration: none;
    display: block;
    margin-bottom: 16px;
  }

  .item-card {
    border-radius: 10px;
    overflow: hidden;
    cursor: pointer;
    transition: transform 0.15s;

    &:hover {
      transform: translateY(-2px);
    }

    .item-image {
      width: 100%;
      height: 160px;
      display: block;
    }

    .image-placeholder {
      width: 100%;
      height: 160px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f7fa;
      color: #c0c4cc;
    }

    .item-info {
      padding: 8px 10px;

      .item-title {
        font-size: 13px;
        color: #303133;
        margin: 0 0 4px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .item-price {
        font-size: 14px;
        font-weight: 600;
        color: #f56c6c;
        margin: 0;
      }
    }
  }
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
