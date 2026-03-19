import request from '@/api/index'

/**
 * 广场动态API
 */
export const squareApi = {
  /**
   * 分享动态（share_count +1）
   */
  sharePost(postId) {
    return request({
      url: `/post/${postId}/share`,
      method: 'post'
    })
  },

  /**
   * 获取单个动态详情
   * @param {number} postId 动态ID
   * @returns {Promise}
   */
  getPostDetail(postId) {
    return request({
      url: `/post/${postId}`,
      method: 'get'
    })
  },

  /**
   * 获取动态评论列表
   * @param {number} postId 动态ID
   * @returns {Promise}
   */
  getComments(postId) {
    return request({
      url: `/square/comments/${postId}`,
      method: 'get'
    })
  },

  /**
   * 获取动态列表
   * @param {Object} params 查询参数
   * @param {number} params.page 页码
   * @param {number} params.size 每页大小
   * @param {string} params.sort 排序方式：recommend(推荐) | hot(热门) | latest(最新)
   * @param {string} params.tag 标签筛选
   * @param {string} params.keyword 关键词搜索
   * @returns {Promise}
   */
  getPosts(params) {
    return request({
      url: '/post/list',
      method: 'get',
      params
    })
  },

  /**
   * 点赞动态
   * @param {number} postId 动态ID
   * @returns {Promise}
   */
  likePost(postId, config) {
    return request({
      url: `/post/${postId}/like`,
      method: 'post',
      ...config
    })
  },

  unlikePost(postId, config) {
    return request({
      url: `/post/${postId}/like`,
      method: 'delete',
      ...config
    })
  },

  favoritePost(postId, config) {
    return request({
      url: `/post/${postId}/favorite`,
      method: 'post',
      ...config
    })
  },

  unfavoritePost(postId, config) {
    return request({
      url: `/post/${postId}/favorite`,
      method: 'delete',
      ...config
    })
  },

  /**
   * 获取动态评论
   * @param {number} postId 动态ID
   * @param {Object} params 分页参数
   * @returns {Promise}
   */
  getComments(postId, params) {
    return request({
      url: `/square/comments/${postId}`,
      method: 'get',
      params
    })
  },

  /**
   * 发表评论
   * @param {Object} data 评论数据
   * @param {number} data.postId 动态ID
   * @param {string} data.content 评论内容
   * @param {number} data.parentId 父评论ID（可选）
   * @returns {Promise}
   */
  addComment(data) {
    return request({
      url: '/square/comment',
      method: 'post',
      data
    })
  },

  /**
   * 删除评论
   * @param {number} commentId 评论ID
   * @returns {Promise}
   */
  deleteComment(commentId) {
    return request({
      url: `/square/comment/${commentId}`,
      method: 'delete'
    })
  },

  /**
   * 获取热门标签
   * @returns {Promise}
   */
  getHotTags() {
    return request({
      url: '/square/tags/hot',
      method: 'get'
    })
  },

  /**
   * 获取广场统计数据
   * @returns {Promise}
   */
  getStats() {
    return request({
      url: '/post/stats',
      method: 'get'
    })
  },

  /**
   * 发布动态
   * @param {Object} data 动态数据
   * @param {number} data.type 动态类型：1-发布物品 2-换物成功 3-分享动态 4-求换动态
   * @param {string} data.content 动态内容
   * @param {number} data.itemId 关联物品ID（类型1需要）
   * @param {number} data.swapRecordId 换物记录ID（类型2需要）
   * @param {Array} data.tagIds 标签ID数组
   * @returns {Promise}
   */
  createPost(data) {
    return request({
      url: '/post/create',
      method: 'post',
      data
    })
  },

  /**
   * 获取可发布的物品列表（用户自己的物品）
   * @returns {Promise}
   */
  getMyItemsForPost() {
    return request({
      url: '/item/my',
      method: 'get',
      params: {
        page: 1,
        size: 100 // 获取所有物品，不需要分页
      }
    })
  },

  /**
   * 获取我的收藏动态列表
   * @param {Object} params { page, size }
   * @returns {Promise}
   */
  getMyFavorites(params) {
    return request({
      url: '/post/favorites',
      method: 'get',
      params
    })
  },

  /**
   * 编辑动态（只能编辑自己的动态）
   * @param {number} postId 动态ID
   * @param {Object} data 要更新的内容
   * @param {string} data.content 动态内容
   * @param {Array} data.imageList 图片列表
   * @returns {Promise}
   */
  updatePost(postId, data) {
    return request({
      url: `/post/${postId}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除动态
   * @param {number} postId 动态ID
   * @returns {Promise}
   */
  deletePost(postId) {
    return request({
      url: `/post/${postId}`,
      method: 'delete'
    })
  },

  /**
   * 获取换物记录列表（用于发布换物成功动态）
   * @returns {Promise}
   */
  getMySwapRecords() {
    return request({
      url: '/post/my-swaps',
      method: 'get'
    })
  },

  /**
   * 获取指定用户发布的动态列表（公开）
   * @param {number} userId 目标用户ID
   * @param {Object} params 分页参数 { page, size }
   * @returns {Promise}
   */
  getUserPosts(userId, params) {
    return request({
      url: `/post/user/${userId}`,
      method: 'get',
      params
    })
  }
}