<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import { computed } from 'vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const ROLE_LABEL = { 0: '普通用户', 1: '管理员', 2: '超级管理员' }
const roleLabel = computed(() => ROLE_LABEL[userStore.userInfo?.role] ?? '')

const menuItems = computed(() => {
  const items = [
    { index: '/admin/users', label: '用户管理', icon: 'User' },
    { index: '/admin/items', label: '商品管理', icon: 'Goods' },
  ]
  return items
})

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="admin-wrapper">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="aside">
      <div class="aside-logo">管理控制台</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="#ffffffa0"
        active-text-color="#fff"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.index"
          :index="item.index"
        >
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="admin-header">
        <el-button link @click="router.push('/')">← 返回前台</el-button>
        <div class="header-right">
          <el-tag :type="userStore.isSuperAdmin ? 'danger' : 'warning'" size="small">
            {{ roleLabel }}
          </el-tag>
          <span class="username">{{ userStore.userInfo?.nickname }}</span>
          <el-button link type="danger" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.admin-wrapper {
  min-height: 100vh;
}

.aside {
  background: #001529;

  .aside-logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    border-bottom: 1px solid #ffffff18;
  }
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 20px;

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .username {
    font-size: 14px;
    color: #606266;
  }
}

.admin-main {
  background: #f5f7fa;
  padding: 20px;
}
</style>
