<script setup>
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'

const router = useRouter()
const route = useRoute()
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
      
      <!-- 导航菜单 -->
      <div class="header-nav">
        <RouterLink :to="{ name: 'Home' }" class="nav-item" :class="{ active: route.name === 'Home' }">首页</RouterLink>
        <RouterLink :to="{ name: 'Category' }" class="nav-item" :class="{ active: route.name === 'Category' }">物品分类</RouterLink>
        <RouterLink :to="{ name: 'Publish' }" class="nav-item" :class="{ active: route.name === 'Publish' }">发布闲置</RouterLink>
        <RouterLink :to="{ name: 'Square' }" class="nav-item" :class="{ active: route.name === 'Square' }">广场</RouterLink>
      </div>
      
      <div class="header-right">
        <template v-if="userStore.isLoggedIn">
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
  background: rgb(27, 153, 170);
  padding: 0 32px;
  height: 64px;
  box-shadow: 0 4px 20px rgba(27, 153, 170, 0.2);
  border-radius: 16px;
  margin: 12px 12px 0 12px;
  position: sticky;
  top: 12px;
  z-index: 1000;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 6px 30px rgba(27, 153, 170, 0.3);
    transform: translateY(-2px);
  }

  .logo {
    font-size: 22px;
    font-weight: 700;
    color: #fff;
    text-decoration: none;
    letter-spacing: 0.5px;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    transition: all 0.3s ease;

    &:hover {
      color: #ffd700;
      transform: scale(1.05);
    }
  }

  .header-nav {
    display: flex;
    align-items: center;
    gap: 32px;
    margin: 0 32px;
    
    .nav-item {
      color: rgba(255, 255, 255, 0.85);
      text-decoration: none;
      font-size: 15px;
      font-weight: 500;
      padding: 8px 0;
      position: relative;
      transition: all 0.3s ease;
      
      &:hover {
        color: #ffd700;
        transform: translateY(-2px);
      }
      
      &.active {
        color: #ffd700;
        font-weight: 600;
        
        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          height: 2px;
          background: #ffd700;
          border-radius: 1px;
          animation: slideIn 0.3s ease;
        }
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .user-avatar {
    display: flex;
    align-items: center;
    cursor: pointer;
    color: #fff;
    font-size: 14px;
    font-weight: 500;
    padding: 8px 16px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 50px;
    transition: all 0.3s ease;
    border: 1px solid rgba(255, 255, 255, 0.2);

    &:hover {
      background: rgba(255, 255, 255, 0.25);
      color: #ffd700;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .el-icon {
      margin-left: 6px;
      transition: transform 0.3s ease;
    }

    &:hover .el-icon {
      transform: rotate(180deg);
    }
  }

  :deep(.el-button) {
    border-radius: 50px;
    font-weight: 500;
    transition: all 0.3s ease;
    border: none;

    &.el-button--primary {
      background: linear-gradient(135deg, #ff9a9e 0%, #fad0c4 100%);
      color: #333;
      box-shadow: 0 4px 15px rgba(255, 154, 158, 0.3);

      &:hover {
        transform: translateY(-3px);
        box-shadow: 0 6px 20px rgba(255, 154, 158, 0.4);
      }
    }

    &.el-button--default {
      background: rgba(255, 255, 255, 0.2);
      color: #fff;
      border: 1px solid rgba(255, 255, 255, 0.3);

      &:hover {
        background: rgba(255, 255, 255, 0.3);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(255, 255, 255, 0.2);
      }
    }
  }

  :deep(.el-dropdown) {
    .el-dropdown-link {
      display: flex;
      align-items: center;
    }
  }
  
  // 导航项下划线动画
  @keyframes slideIn {
    from {
      transform: scaleX(0);
      opacity: 0;
    }
    to {
      transform: scaleX(1);
      opacity: 1;
    }
  }
}

.main-content {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  padding: 24px;
  min-height: calc(100vh - 180px);
  margin: 0 12px;
  border-radius: 16px;
  margin-top: 12px;
}

.footer {
  text-align: center;
  color: #909399;
  font-size: 13px;
  line-height: 60px;
  background: #fff;
  border-radius: 16px;
  margin: 12px;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.05);
}

// 响应式调整
@media (max-width: 768px) {
  .header {
    padding: 0 16px;
    border-radius: 0 0 12px 12px;

    .logo {
      font-size: 18px;
    }

    .header-nav {
      display: none; // 在小屏幕上隐藏导航菜单，或者可以改为下拉菜单
    }

    .header-right {
      gap: 8px;
    }

    .user-avatar {
      padding: 6px 12px;
      font-size: 13px;
    }
  }

  .main-content {
    padding: 16px;
  }
}
</style>
