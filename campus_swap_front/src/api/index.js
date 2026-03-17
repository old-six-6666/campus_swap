import axios from 'axios'
import { showError } from '@/utils/notify'
import { useUserStore } from '@/stores/useUserStore'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

// 请求拦截器：自动附带 JWT Token
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理业务错误与 Token 失效
// 请求 config 中可传 { silent: true } 来静默处理错误（不弹出提示）
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    if (!response.config?.silent) {
      showError(message || '请求失败')
    }
    return Promise.reject(new Error(message))
  },
  (error) => {
    if (error.response?.status === 401 && !error.config?.silent) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
    } else if (error.response?.status !== 401 && !error.config?.silent) {
      showError(error.response?.data?.message || '网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
