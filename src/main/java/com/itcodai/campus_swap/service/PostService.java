package com.itcodai.campus_swap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcodai.campus_swap.entity.Post;
import com.itcodai.campus_swap.vo.PageVO;

import java.util.Map;

/**
 * 动态服务接口
 */
public interface PostService extends IService<Post> {
    
    /**
     * 分页查询动态列表
     * @param page 页码
     * @param size 每页大小
     * @param sort 排序方式：recommend-推荐 latest-最新 hot-热门
     * @param keyword 搜索关键词
     * @param tag 标签筛选
     * @return 分页结果
     */
    PageVO<Map<String, Object>> getPosts(Integer page, Integer size, String sort, String keyword, String tag);
    
    /**
     * 点赞动态
     * @param postId 动态ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean likePost(Long postId, Long userId);
    
    /**
     * 取消点赞
     * @param postId 动态ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean unlikePost(Long postId, Long userId);
    
    /**
     * 收藏动态
     * @param postId 动态ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean favoritePost(Long postId, Long userId);
    
    /**
     * 取消收藏
     * @param postId 动态ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean unfavoritePost(Long postId, Long userId);
    
    /**
     * 创建动态
     * @param post 动态信息
     * @param userId 用户ID
     * @return 动态ID
     */
    Long createPost(Post post, Long userId);
    
    /**
     * 获取动态统计数据
     * @return 统计数据
     */
    Map<String, Object> getStats();
    
    /**
     * 删除动态（只能删除自己的动态）
     * @param postId 动态ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deletePost(Long postId, Long userId);
}