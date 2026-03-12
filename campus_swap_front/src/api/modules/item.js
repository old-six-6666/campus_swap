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
}
