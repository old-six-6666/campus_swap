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
     */
    PageVO<Map<String, Object>> getPosts(Integer page, Integer size, String sort, String keyword, String tag, Long currentUserId);

    /**
     * 获取单个动态详情
     */
    Map<String, Object> getPostDetail(Long postId, Long currentUserId);

    /** 浏览量 +1 并刷新热度（异步调用） */
    void incrementView(Long postId);

    /** 分享 +1 并刷新热度 */
    boolean sharePost(Long postId);

    boolean likePost(Long postId, Long userId);
    boolean unlikePost(Long postId, Long userId);
    boolean favoritePost(Long postId, Long userId);
    boolean unfavoritePost(Long postId, Long userId);
    Long createPost(Post post, Long userId);
    Map<String, Object> getStats();
    boolean deletePost(Long postId, Long userId);

    /** 查询指定用户发布的动态列表（公开） */
    PageVO<Map<String, Object>> getUserPosts(Long userId, int page, int size, Long currentUserId);
}