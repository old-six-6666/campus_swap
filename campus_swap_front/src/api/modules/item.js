import request from '@/api/index'

export const itemApi = {
  /** 分页查询商品列表 */
  getList: (params) => request.get('/item/list', { params }),

  /** 获取商品详情 */
  getDetail: (id) => request.get(`/item/${id}`),

  /** 发布商品 */
  publish: (data) => request.post('/item/publish', data),

  /** 更新商品 */
  update: (id, data) => request.put(`/item/${id}`, data),

  /** 删除商品 */
  remove: (id) => request.delete(`/item/${id}`),

  /** 搜索商品（ES） */
  search: (params) => request.get('/item/search', { params }),

  /** 查询当前用户发布的商品列表 */
  getMyItems: (params) => request.get('/item/my', { params }),

  /** 查询指定用户已审核通过且在售的商品列表（用于用户主页） */
  getUserItems: (userId, params) => request.get(`/item/user/${userId}`, { params }),

  /** 上传图片，返回可访问的 URL */
  uploadImage: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

export const exchangeApi = {
  /** 以物换物匹配查询 */
  match: (data) => request.post('/exchange/match', data),

  /** 将物品同步到 ES */
  syncItem: (itemId) => request.post(`/exchange/sync/${itemId}`),

  /** 初始化 ES 索引（超级管理员） */
  initIndex: () => request.post('/exchange/init-index'),
}

