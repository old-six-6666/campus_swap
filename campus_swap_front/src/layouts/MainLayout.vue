<script setup>
import { RouterView, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'

const router = useRouter()
const userStore = useUserStore()

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'myItems') {
    router.push('/my-items')
  } else if (command === 'changePassword') {
    router.push('/change-password')
  } else if (command === 'admin') {
    router.push('/admin')
  }
}
</script>

<template>
  <el-container class="layout-wrapper">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="header-left">
        <RouterLink to="/" class="logo">换了吗</RouterLink>
      </div>
      <div class="header-right">
        <template v-if="userStore.isLoggedIn">
          <RouterLink to="/publish">
            <el-button type="primary" size="small">发布闲置</el-button>
          </RouterLink>
          <el-dropdown @command="handleCommand">
            <span class="user-avatar">
              <el-tag
                v-if="userStore.isAdmin"
                :type="userStore.isSuperAdmin ? 'danger' : 'warning'"
                size="small"
                style="margin-right:6px"
              >{{ userStore.isSuperAdmin ? '超管' : '管理员' }}</el-tag>
              {{ userStore.userInfo?.nickname || '用户' }}
              <el-icon style="margin-left:2px;vertical-align:middle;"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="myItems">我的闲置</el-dropdown-item>
                <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>
                  管理面板
                </el-dropdown-item>
                <el-dropdown-item command="logout" :divided="!userStore.isAdmin">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <RouterLink to="/login">
            <el-button size="small">登录</el-button>
          </RouterLink>
          <RouterLink to="/register">
            <el-button type="primary" size="small">注册</el-button>
          </RouterLink>
        </template>
      </div>
    </el-header>

    <!-- 主内容区 -->
    <el-main class="main-content">
      <RouterView />
    </el-main>

    <!-- 底部 -->
    <el-footer class="footer">© 2025 换了吗</el-footer>
  </el-container>
</template>

<style scoped lang="scss">
.layout-wrapper {
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 24px;

  .logo {
    font-size: 18px;
    font-weight: 600;
    color: #409eff;
    text-decoration: none;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .user-avatar {
    display: flex;
    align-items: center;
    cursor: pointer;
    color: #606266;
    font-size: 14px;

    &:hover {
      color: #409eff;
    }
  }
}

.main-content {
  background: #f5f7fa;
  padding: 24px;
}

.footer {
  text-align: center;
  color: #909399;
  font-size: 13px;
  line-height: 60px;
}
</style>
