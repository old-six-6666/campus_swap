import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/modules/user'
import { adminApi } from '@/api/modules/admin'

const STORAGE_KEY = 'campus_swap_user'

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : { token: '', userInfo: null, permissions: [] }
  } catch {
    return { token: '', userInfo: null, permissions: [] }
  }
}

export const useUserStore = defineStore('user', () => {
  const saved = loadFromStorage()
  const token = ref(saved.token || '')
  const userInfo = ref(saved.userInfo || null)
  /** 当前管理员的权限码列表（超管为全集，普通用户为空） */
  const permissions = ref(saved.permissions || [])

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role >= 1)
  const isSuperAdmin = computed(() => userInfo.value?.role >= 2)
  /** 判断当前用户是否拥有某权限（超管直接返回 true） */
  function hasPermission(code) {
    return isSuperAdmin.value || permissions.value.includes(code)
  }

  function _persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      token: token.value,
      userInfo: userInfo.value,
      permissions: permissions.value,
    }))
  }

  async function login(credentials) {
    const data = await userApi.login(credentials)
    token.value = data.token
    userInfo.value = data.userInfo
    permissions.value = []
    _persist()
    // 登录后若为管理员则立即加载权限
    if (userInfo.value?.role >= 1) {
      await fetchPermissions()
    }
  }

  async function fetchProfile() {
    userInfo.value = await userApi.getProfile()
    _persist()
  }

  /** 从服务端刷新当前管理员的权限 */
  async function fetchPermissions() {
    if (!isAdmin.value) {
      permissions.value = []
      _persist()
      return
    }
    try {
      permissions.value = await adminApi.getMyPermissions()
    } catch {
      permissions.value = []
    }
    _persist()
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem(STORAGE_KEY)
  }

  return {
    token, userInfo, permissions,
    isLoggedIn, isAdmin, isSuperAdmin,
    hasPermission,
    login, fetchProfile, fetchPermissions, logout,
  }
})
