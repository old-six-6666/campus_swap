<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import { useUnreadStore } from '@/stores/useUnreadStore'
import { tradeApi } from '@/api/modules/trade'
import { userApi } from '@/api/modules/user'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const unreadStore = useUnreadStore()

const messageUnreadCount = computed(() => unreadStore.chatUnread + unreadStore.notifUnread)
const tradeUnreadCount = ref(0)    // 交易菜单：待操作交易
let unreadTimer = null

const userSearchKeyword = ref('')
const userSearchResults = ref([])
const userSearchLoading = ref(false)
const searchPopoverVisible = ref(false)

async function handleUserSearch() {
  const kw = userSearchKeyword.value.trim()
  if (!kw) { userSearchResults.value = []; return }
  userSearchLoading.value = true
  try {
    userSearchResults.value = await userApi.searchUsers(kw)
  } finally {
    userSearchLoading.value = false
  }
}

function goUserHome(userId) {
  searchPopoverVisible.value = false
  userSearchKeyword.value = ''
  userSearchResults.value = []
  router.push({ name: 'UserHome', params: { id: userId } })
}

async function fetchUnread() {
  if (!userStore.isLoggedIn) return
  const [tradeData] = await Promise.all([
    tradeApi.getPendingCount().catch(() => null),
    unreadStore.refresh(),
  ])
  tradeUnreadCount.value = tradeData?.count || 0
}

// 离开消息页时立即刷新（确保红点及时更新）
watch(() => route.name, (name, prev) => {
  if (prev === 'Chat' || prev === 'Notification') {
    fetchUnread()
  }
})

onMounted(() => {
  fetchUnread()
  unreadTimer = setInterval(fetchUnread, 15000)
})

onUnmounted(() => {
  clearInterval(unreadTimer)
})

async function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'myItems') {
    router.push('/my-items')
  } else if (command === 'myFavorites') {
    router.push('/my-favorites')
  } else if (command === 'myTrades') {
    router.push('/trade')
  } else if (command === 'changePassword') {
    router.push('/change-password')
  } else if (command === 'admin') {
    // 跳转前先刷新权限，避免 localStorage 缓存过期导致进不去
    await userStore.fetchPermissions()
    router.push('/admin')
  } else if (command === 'chat') {
    router.push('/chat')
  }
}
</script>

<template>
  <div class="layout-root">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-inner">
        <!-- Logo -->
        <RouterLink to="/" class="logo-link">
          <img src="/logo.png" alt="换了吗" class="logo-img" />
          <span class="logo-text">换了吗</span>
        </RouterLink>

        <!-- 导航菜单 -->
        <nav class="header-nav">
          <RouterLink :to="{ name: 'Home' }" class="nav-item" :class="{ active: route.name === 'Home' }">首页</RouterLink>
          <RouterLink :to="{ name: 'Category' }" class="nav-item" :class="{ active: route.name === 'Category' }">物品分类</RouterLink>
          <RouterLink :to="{ name: 'Publish' }" class="nav-item" :class="{ active: route.name === 'Publish' }">发布闲置</RouterLink>
          <RouterLink :to="{ name: 'Square' }" class="nav-item" :class="{ active: route.name === 'Square' }">广场</RouterLink>
          <RouterLink v-if="userStore.isLoggedIn" :to="{ name: 'Chat' }" class="nav-item" :class="{ active: route.name === 'Chat' }">
            <span class="nav-label">消息<el-badge v-if="messageUnreadCount > 0" :value="messageUnreadCount > 99 ? '99+' : messageUnreadCount" class="nav-badge" /></span>
          </RouterLink>
          <RouterLink v-if="userStore.isLoggedIn" :to="{ name: 'MyTrades' }" class="nav-item" :class="{ active: route.name === 'MyTrades' }">
            <span class="nav-label">交易<el-badge v-if="tradeUnreadCount > 0" :is-dot="true" class="nav-badge" /></span>
          </RouterLink>
        </nav>

        <div class="header-right">
          <!-- 搜索用户 -->
          <el-popover
            :visible="searchPopoverVisible"
            placement="bottom-end"
            :width="300"
            trigger="click"
            @update:visible="searchPopoverVisible = $event"
          >
            <template #reference>
              <button class="search-btn" title="搜索用户">
                <el-icon><Search /></el-icon>
              </button>
            </template>
            <div class="user-search-panel">
              <el-input
                v-model="userSearchKeyword"
                placeholder="搜索用户昵称..."
                size="small"
                clearable
                :prefix-icon="Search"
                @keyup.enter="handleUserSearch"
                @clear="userSearchResults = []"
              />
              <div v-if="userSearchLoading" class="search-loading">搜索中...</div>
              <div v-else-if="userSearchResults.length === 0 && userSearchKeyword" class="search-empty">未找到用户</div>
              <div v-else class="search-results">
                <div
                  v-for="u in userSearchResults"
                  :key="u.id"
                  class="search-result-item"
                  @click="goUserHome(u.id)"
                >
                  <el-avatar :size="34" :src="userStore.getAvatar(u.avatar)" style="object-fit:cover;flex-shrink:0" />
                  <div class="result-info">
                    <span class="result-name">{{ u.nickname }}</span>
                    <span v-if="u.school" class="result-school">{{ u.school }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>

          <template v-if="userStore.isLoggedIn">
            <el-dropdown @command="handleCommand">
              <span class="user-chip">
                <el-avatar
                  :size="28"
                  :src="userStore.getAvatar(userStore.userInfo?.avatar)"
                  class="chip-avatar"
                  style="object-fit:cover"
                />
                <el-tag
                  v-if="userStore.isAdmin"
                  :type="userStore.isSuperAdmin ? 'danger' : 'warning'"
                  size="small"
                  class="role-tag"
                >{{ userStore.isSuperAdmin ? '超管' : '管理员' }}</el-tag>
                {{ userStore.userInfo?.nickname || '用户' }}
                <el-icon style="margin-left:4px;"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="myItems">我的闲置</el-dropdown-item>
                  <el-dropdown-item command="myFavorites">我的收藏</el-dropdown-item>
                  <el-dropdown-item command="myTrades">我的交易</el-dropdown-item>
                  <el-dropdown-item command="chat">
                    消息
                    <el-badge v-if="messageUnreadCount" :value="messageUnreadCount" style="margin-left:6px" />
                  </el-dropdown-item>
                  <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>管理面板</el-dropdown-item>
                  <el-dropdown-item command="logout" :divided="!userStore.isAdmin">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <RouterLink to="/login">
              <el-button class="btn-login" size="small">登录</el-button>
            </RouterLink>
            <RouterLink to="/register">
              <el-button type="primary" size="small">注册</el-button>
            </RouterLink>
          </template>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <RouterView />
    </main>

    <!-- 底部 -->
    <footer class="footer">
      <span>© 2025 换了吗 · 校园闲置交换平台</span>
    </footer>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.layout-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg-page;
}

/* ── 导航栏 ─────────────────────────────── */
.header {
  position: sticky;
  top: 0;
  z-index: 1000;
  padding: 10px 16px 0;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $primary;
  border-radius: 20px;
  padding: 0 28px;
  height: $header-height;
  box-shadow: $shadow-md;
  /* 关键：增加整体过渡动画 */
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.3s ease;

  /* 1. 实现导航栏整体鼠标划入悬浮效果 */
  &:hover {
    transform: translateY(-4px); /* 整体向上浮动 */
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15); /* 增加更柔和的深层阴影 */
  }
}

/* Logo */
.logo-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-img {
  height: 50px;
  width: 50px;
  object-fit: contain;
  /* 2. 取消 Logo 独自的特效（移除了之前的 transition） */
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: $letter-spacing-wide;
}
.logo-link:hover .logo-text {
  color: $warning;
}
/* 导航项 */
.header-nav {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  justify-content: center;
}

.nav-item {
  color: rgba(255, 255, 255, 0.82);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding: 7px 14px;
  border-radius: 50px;
  letter-spacing: $letter-spacing-base;
  transition: all 0.2s ease; /* 统一过渡 */
  white-space: nowrap;

  /* 3. 划入导航项时：取消局部浮动，改为和激活状态一样的背景效果 */
  &:hover {
    color: $primary;       /* 变成主色调文字 */
    background: #fff;      /* 变成纯白背景 */
    transform: none;       /* 强制取消之前的 translateY 浮动 */
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    .nav-dot { border-color: #fff; }
  }

  /* 保持激活状态（点击后）的效果 */
  &.active {
    color: $primary;
    background: #fff;
    font-weight: 600;
    box-shadow: 0 2px 8px rgba(0,0,0,0.12);
    .nav-dot { border-color: #fff; }
  }


  :deep(.el-badge__content) {
    top: -4px;
    right: -14px;
  }
}

.nav-label {
  position: relative;
  display: inline-block;
}

.nav-badge {
  position: absolute;
  top: -8px;
  right: -18px;
  :deep(.el-badge__content) {
    border-color: transparent;
  }
}

.msg-badge {
  :deep(.el-badge__content) {
    top: -6px;
    right: -18px;
  }
}

/* 右侧操作区 */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #fff;
  cursor: pointer;
  transition: $transition-fast;
  font-size: 16px;

  &:hover {
    background: rgba(255, 255, 255, 0.28);
    transform: scale(1.08);
  }
}

.user-chip {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  padding: 5px 14px 5px 5px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  transition: $transition-fast;
  letter-spacing: $letter-spacing-base;
  gap: 8px;

  &:hover {
    background: rgba(255, 255, 255, 0.25);
    color: $warning;
  }

  .chip-avatar {
    flex-shrink: 0;
    border: 2px solid rgba(255, 255, 255, 0.5);
  }

  // 超管标签：暖金色，与深青底形成色相对比
  :deep(.role-tag.el-tag--danger) {
    background:   #F1C65E !important;
    color:        #5a3a00 !important;
    border-color: #d4a017 !important;
    font-weight:  700 !important;
    border-radius: 6px !important;
    letter-spacing: 0.05em !important;
    padding: 0 7px !important;
  }
  // 普通管理员标签：冷光银，与超管暖金形成呼应
  :deep(.role-tag.el-tag--warning) {
    background:   linear-gradient(135deg, #f0f4f8 0%, #b8ccd8 40%, #dce8ef 70%, #f0f4f8 100%) !important;
    color:        #1a3a4a !important;
    border-color: rgba(160, 200, 220, 0.7) !important;
    font-weight:  700 !important;
    border-radius: 6px !important;
    letter-spacing: 0.05em !important;
    padding: 0 7px !important;
    box-shadow: 0 1px 4px rgba(27,153,170,0.18), inset 0 1px 0 rgba(255,255,255,0.85) !important;
    text-shadow: 0 1px 0 rgba(255,255,255,0.8) !important;
  }
}

.btn-login {
  background: rgba(255,255,255,0.15) !important;
  border: 1px solid rgba(255,255,255,0.3) !important;
  color: #fff !important;
  &:hover { background: rgba(255,255,255,0.28) !important; }
}

/* ── 主内容 ────────────────────────────── */
.main-content {
  flex: 1;
  padding: 20px 16px;
  max-width: calc($max-content-width + 32px);
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

/* ── 页脚 ──────────────────────────────── */
.footer {
  text-align: center;
  color: $text-secondary;
  font-size: 13px;
  padding: 18px;
  letter-spacing: $letter-spacing-base;
  border-top: 1px solid $border-color;
  background: $bg-card;
  margin-top: 8px;
}

/* ── 响应式 ────────────────────────────── */
@media (max-width: 768px) {
  .header { padding: 8px 8px 0; }

  .header-inner {
    padding: 0 16px;
    border-radius: $border-radius;
  }

  .logo-text { font-size: 17px; }

  .header-nav {
    gap: 2px;
    .nav-item {
      padding: 6px 10px;
      font-size: 13px;
    }
  }

  .main-content { padding: 14px 10px; }
}

@media (max-width: 480px) {
  .header-nav {
    display: none;
  }

  .logo-text { font-size: 16px; }
}
</style>

<style lang="scss">
@import '@/assets/styles/variables.scss';

/* 搜索面板（全局，弹出层在 teleport 中） */
.user-search-panel {
  padding: 6px 0;

  .el-input { margin-bottom: 10px; }

  .search-loading,
  .search-empty {
    text-align: center;
    font-size: 13px;
    color: $text-secondary;
    padding: 14px 0;
  }

  .search-results { max-height: 260px; overflow-y: auto; }

  .search-result-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 6px;
    cursor: pointer;
    border-radius: 12px;
    transition: background 0.15s;

    &:hover { background: rgba(27, 153, 170, 0.08); }

    .result-info {
      display: flex;
      flex-direction: column;
      .result-name  { font-size: 14px; color: $text-primary; font-weight: 500; }
      .result-school { font-size: 12px; color: $text-secondary; margin-top: 2px; }
    }
  }
}
</style>