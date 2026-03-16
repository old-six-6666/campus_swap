import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/HomeView.vue'),
      },
      {
        path: 'item/:id',
        name: 'ItemDetail',
        component: () => import('@/views/item/ItemDetailView.vue'),
      },
      {
        path: 'publish',
        name: 'Publish',
        component: () => import('@/views/item/PublishView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'my-items',
        name: 'MyItems',
        component: () => import('@/views/item/MyItemsView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'item/edit/:id',
        name: 'EditItem',
        component: () => import('@/views/item/EditView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/user/ChangePasswordView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('@/views/category/CategoryView.vue'),
      },
      {
        path: 'square',
        name: 'Square',
        component: () => import('@/views/square/SquareView.vue'),
      },
      {
        path: 'square/post/:id',
        name: 'PostDetail',
        component: () => import('@/views/square/PostDetailView.vue'),
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/ChatView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'trade',
        name: 'MyTrades',
        component: () => import('@/views/trade/TradeView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'trade/:id',
        name: 'TradeDetail',
        component: () => import('@/views/trade/TradeDetailView.vue'),
        meta: { requiresAuth: true },
      },
    ],
  },
  // ===== 管理端（需要 admin role） =====
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManageView.vue'),
        meta: { requiresPermission: 'USER_MANAGE' },
      },
      {
        path: 'items',
        name: 'AdminItems',
        component: () => import('@/views/admin/ItemManageView.vue'),
        meta: { requiresPermission: 'ITEM_MANAGE' },
      },
      {
        path: 'items/audit',
        name: 'ItemAudit',
        component: () => import('@/views/admin/ItemAuditView.vue'),
        meta: { requiresPermission: 'ITEM_AUDIT' },
      },
      {
        path: 'admins',
        name: 'AdminManage',
        component: () => import('@/views/admin/AdminManageView.vue'),
        meta: { requiresSuperAdmin: true },
      },
      {
        path: 'students',
        name: 'StudentManage',
        component: () => import('@/views/admin/StudentManageView.vue'),
        meta: { requiresPermission: 'STUDENT_MANAGE' },
      },
      {
        path: 'verifications',
        name: 'VerifyManage',
        component: () => import('@/views/admin/VerifyManageView.vue'),
        meta: { requiresPermission: 'STUDENT_MANAGE' },
      },
    ],
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPasswordView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

/** 按用户权限返回管理台的第一个可访问路由名称，无权限时返回 null */
function firstAdminRoute(userStore) {
  if (userStore.hasPermission('USER_MANAGE')) return { name: 'AdminUsers' }
  if (userStore.hasPermission('ITEM_MANAGE')) return { name: 'AdminItems' }
  if (userStore.hasPermission('ITEM_AUDIT'))  return { name: 'ItemAudit' }
  if (userStore.isSuperAdmin)                 return { name: 'AdminManage' }
  return null
}

// 路由守卫
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return { name: 'Home' }
  }
  // /admin 根路径：动态跳转到第一个有权访问的子页面
  if (to.path === '/admin') {
    return firstAdminRoute(userStore) ?? { name: 'Home' }
  }
  // 超级管理员专属页面
  if (to.meta.requiresSuperAdmin && !userStore.isSuperAdmin) {
    return firstAdminRoute(userStore) ?? { name: 'Home' }
  }
  // 细粒度权限检查（超管直接放行）
  if (to.meta.requiresPermission && !userStore.hasPermission(to.meta.requiresPermission)) {
    return firstAdminRoute(userStore) ?? { name: 'Home' }
  }
  if (to.meta.guestOnly && userStore.isLoggedIn) {
    return { name: 'Home' }
  }
})

export default router
