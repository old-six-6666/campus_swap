import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/modules/user'

const STORAGE_KEY = 'campus_swap_user'

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : { token: '', userInfo: null }
  } catch {
    return { token: '', userInfo: null }
  }
}

export const useUserStore = defineStore('user', () => {
  const saved = loadFromStorage()
  const token = ref(saved.token || '')
  const userInfo = ref(saved.userInfo || null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role >= 1)
  const isSuperAdmin = computed(() => userInfo.value?.role >= 2)

  function _persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: token.value, userInfo: userInfo.value }))
  }

  async function login(credentials) {
    const data = await userApi.login(credentials)
    token.value = data.token
    userInfo.value = data.userInfo
    _persist()
  }

  async function fetchProfile() {
    userInfo.value = await userApi.getProfile()
    _persist()
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  return { token, userInfo, isLoggedIn, isAdmin, isSuperAdmin, login, fetchProfile, logout }
})
