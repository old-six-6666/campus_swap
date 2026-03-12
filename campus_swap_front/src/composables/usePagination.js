import { ref, reactive } from 'vue'

/**
 * 分页查询 composable
 * @param {Function} fetchFn - 异步查询函数，接收 params 返回 { records, total }
 * @param {Object} defaultParams - 默认查询参数
 */
export function usePagination(fetchFn, defaultParams = {}) {
  const loading = ref(false)
  const list = ref([])
  const pagination = reactive({ page: 1, size: 10, total: 0 })
  const params = reactive({ ...defaultParams })

  async function fetchData() {
    loading.value = true
    try {
      const data = await fetchFn({ ...params, page: pagination.page, size: pagination.size })
      list.value = data.records || []
      pagination.total = data.total || 0
    } finally {
      loading.value = false
    }
  }

  function onPageChange(page) {
    pagination.page = page
    fetchData()
  }

  function reset() {
    Object.assign(params, defaultParams)
    pagination.page = 1
    fetchData()
  }

  return { loading, list, pagination, params, fetchData, onPageChange, reset }
}
