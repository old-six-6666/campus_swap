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

  /** 上传图片，返回可访问的 URL */
  uploadImage: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/api/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
