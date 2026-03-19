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

const items = ref([])
const itemsLoading = ref(false)
const itemsPage = ref(1)
const itemsTotal = ref(0)
const itemsSize = 12

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
  if (tab === 'posts' && posts.value.length === 0) fetchPosts(1)
}

function contactUser() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  router.push({
    name: 'Chat',
    query: { to: userInfo.value.id, nickname: userInfo.value.nickname, avatar: userInfo.value.avatar || '' },
  })
}

onMounted(async () => {
  await fetchUser()
  await fetchItems(1)
})

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
      <div class="profile-card">
        <div class="profile-bg"></div>
        <div class="profile-body">
          <el-avatar :size="80" :src="userStore.getAvatar(userInfo.avatar)" class="profile-avatar" style="object-fit:cover">
            <el-icon :size="40"><UserIcon /></el-icon>
          </el-avatar>
          <div class="profile-info">
            <div class="nickname-row">
              <span class="nickname">{{ userInfo.nickname }}</span>
              <el-tag v-if="userInfo.isVerified === 1" size="small" effect="plain">已认证</el-tag>
            </div>
            <div v-if="userInfo.school" class="school">
              {{ userInfo.school }}
            </div>
          </div>
          <div class="profile-actions">
            <el-button
              v-if="!isSelf && userStore.isLoggedIn"
              type="primary"
              :icon="ChatDotRound"
              round
              @click="contactUser"
            >私信</el-button>
            <el-button
              v-if="isSelf"
              round
              @click="router.push({ name: 'Profile' })"
            >编辑资料</el-button>
          </div>
        </div>
      </div>

      <!-- Tab 切换 -->
      <div class="content-panel">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <!-- 在售物品 -->
          <el-tab-pane label="在售物品" name="items">
            <div v-loading="itemsLoading" class="items-grid">
              <el-empty v-if="!itemsLoading && items.length === 0" description="暂无在售物品" :image-size="80" />
              <template v-else>
                <RouterLink
                  v-for="item in items"
                  :key="item.id"
                  :to="`/item/${item.id}`"
                  class="item-card"
                >
                  <div class="card-img-wrap">
                    <el-image :src="item.coverImage" fit="cover" class="card-img" lazy>
                      <template #error>
                        <div class="img-placeholder"><el-icon size="24"><Picture /></el-icon></div>
                      </template>
                    </el-image>
                  </div>
                  <div class="card-info">
                    <p class="card-title">{{ item.title }}</p>
                    <p class="card-price">¥ {{ item.price }}</p>
                  </div>
                </RouterLink>
              </template>
            </div>
            <div v-if="itemsTotal > itemsSize" class="pagination-wrap">
              <el-pagination
                :current-page="itemsPage"
                :page-size="itemsSize"
                :total="itemsTotal"
                layout="prev, pager, next"
                @current-change="fetchItems"
              />
            </div>
          </el-tab-pane>

          <!-- 发布动态 -->
          <el-tab-pane label="发布动态" name="posts">
            <div v-loading="postsLoading" class="posts-list">
              <el-empty v-if="!postsLoading && posts.length === 0" description="暂无动态" :image-size="80" />
              <SquareCard v-for="post in posts" :key="post.id" :post="post" />
            </div>
            <div v-if="postsTotal > postsSize" class="pagination-wrap">
              <el-pagination
                :current-page="postsPage"
                :page-size="postsSize"
                :total="postsTotal"
                layout="prev, pager, next"
                @current-change="fetchPosts"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.user-home {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── 用户信息卡 ─────────────────────────── */
.profile-card {
  background: $bg-card;
  border-radius: $border-radius-lg;
  overflow: hidden;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;
}

.profile-bg {
  height: 88px;
  background: linear-gradient(135deg, $primary 0%, $primary-light 100%);
}

.profile-body {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  padding: 0 28px 24px;
  margin-top: -40px;
  flex-wrap: wrap;
}

.profile-avatar {
  border: 4px solid #fff;
  box-shadow: $shadow-sm;
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
  min-width: 0;
  padding-top: 44px;

  .nickname-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
    flex-wrap: wrap;

    .nickname {
      font-size: 20px;
      font-weight: 700;
      color: $text-primary;
      letter-spacing: $letter-spacing-base;
    }
  }

  .school {
    font-size: 13px;
    color: $text-secondary;
    letter-spacing: $letter-spacing-base;
  }
}

.profile-actions {
  padding-top: 44px;
  flex-shrink: 0;
}

/* ── 内容面板 ───────────────────────────── */
.content-panel {
  background: $bg-card;
  border-radius: $border-radius-lg;
  padding: 16px 24px 24px;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;
}

/* ── 物品网格 ───────────────────────────── */
.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 16px;
  min-height: 140px;
}

.item-card {
  text-decoration: none;
  display: block;
  background: $bg-card;
  border-radius: $border-radius;
  overflow: hidden;
  border: 1px solid $border-color;
  box-shadow: $shadow-card;
  transition: $transition-base;

  &:hover {
    transform: translateY(-3px);
    box-shadow: $shadow-md;
    border-color: $primary-light;
  }
}

.card-img-wrap {
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
}

.card-img {
  width: 100%;
  height: 100%;
  transition: transform 0.4s ease;
  .item-card:hover & { transform: scale(1.04); }
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-subtle;
  color: $text-secondary;
}

.card-info {
  padding: 10px 12px;

  .card-title {
    font-size: 13px;
    color: $text-primary;
    font-weight: 500;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    letter-spacing: $letter-spacing-base;
  }

  .card-price {
    font-size: 14px;
    font-weight: 700;
    color: $warning;
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
  margin-top: 24px;
}

@media (max-width: 600px) {
  .profile-body { padding: 0 16px 20px; }
  .content-panel { padding: 14px 14px 20px; }
  .items-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
}
</style>