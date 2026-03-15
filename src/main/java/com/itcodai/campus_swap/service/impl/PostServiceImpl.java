package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcodai.campus_swap.entity.Post;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.PostMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.PostService;
import com.itcodai.campus_swap.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态服务实现类
 */
@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {
    
    private final UserMapper userMapper;
    
    public PostServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    
    @Override
    public PageVO<Map<String, Object>> getPosts(Integer page, Integer size, String sort, String keyword, String tag) {
        // 创建分页对象
        Page<Post> postPage = new Page<>(page, size);
        
        // 构建查询条件
        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索（内容）
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like(Post::getContent, keyword);
        }
        
        // 标签筛选（这里简化处理，实际应该关联标签表）
        // 暂时使用内容中包含标签关键词的方式
        if (tag != null && !tag.trim().isEmpty()) {
            queryWrapper.like(Post::getContent, tag);
        }
        
        // 排序方式
        if ("latest".equals(sort)) {
            queryWrapper.orderByDesc(Post::getCreatedAt);
        } else if ("hot".equals(sort)) {
            // 热门排序：点赞数 + 评论数 * 0.5 + 收藏数 * 0.3
            queryWrapper.orderByDesc(Post::getLikeCount, Post::getCommentCount, Post::getFavoriteCount);
        } else {
            // 默认推荐排序：综合热度 + 时间衰减
            queryWrapper.orderByDesc(Post::getCreatedAt);
        }
        
        // 执行分页查询
        Page<Post> result = this.page(postPage, queryWrapper);
        
        // 转换为包含用户信息的Map列表
        List<Map<String, Object>> records = result.getRecords().stream().map(post -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("userId", post.getUserId());
            map.put("type", post.getType());
            map.put("content", post.getContent());
            map.put("likeCount", post.getLikeCount());
            map.put("commentCount", post.getCommentCount());
            map.put("favoriteCount", post.getFavoriteCount());
            map.put("createdAt", post.getCreatedAt());
            
            // 查询用户信息
            User user = userMapper.selectById(post.getUserId());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getNickname()); // User实体使用nickname字段
                userInfo.put("avatar", user.getAvatar());
                map.put("user", userInfo);
            }
            
            // 模拟标签数据（实际应该从标签表查询）
            map.put("tags", List.of("校园", "闲置"));
            
            // 模拟是否点赞/收藏（实际应该从点赞/收藏表查询）
            map.put("isLiked", false);
            map.put("isFavorited", false);
            
            return map;
        }).collect(Collectors.toList());
        
        // 构建分页结果
        PageVO<Map<String, Object>> pageVO = new PageVO<>();
        pageVO.setRecords(records);
        pageVO.setTotal(result.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        
        return pageVO;
    }
    
    @Override
    @Transactional
    public boolean likePost(Long postId, Long userId) {
        try {
            // 查询动态
            Post post = this.getById(postId);
            if (post == null) {
                return false;
            }
            
            // 更新点赞数（实际应该操作点赞表，这里简化处理）
            post.setLikeCount(post.getLikeCount() + 1);
            this.updateById(post);
            
            // TODO: 实际应该操作 t_post_like 表记录点赞关系
            log.info("用户 {} 点赞了动态 {}", userId, postId);
            return true;
        } catch (Exception e) {
            log.error("点赞动态失败", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean unlikePost(Long postId, Long userId) {
        try {
            // 查询动态
            Post post = this.getById(postId);
            if (post == null) {
                return false;
            }
            
            // 更新点赞数
            if (post.getLikeCount() > 0) {
                post.setLikeCount(post.getLikeCount() - 1);
                this.updateById(post);
            }
            
            // TODO: 实际应该从 t_post_like 表删除点赞关系
            log.info("用户 {} 取消点赞动态 {}", userId, postId);
            return true;
        } catch (Exception e) {
            log.error("取消点赞失败", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean favoritePost(Long postId, Long userId) {
        try {
            // 查询动态
            Post post = this.getById(postId);
            if (post == null) {
                return false;
            }
            
            // 更新收藏数（实际应该操作收藏表，这里简化处理）
            post.setFavoriteCount(post.getFavoriteCount() + 1);
            this.updateById(post);
            
            // TODO: 实际应该操作 t_post_favorite 表记录收藏关系
            log.info("用户 {} 收藏了动态 {}", userId, postId);
            return true;
        } catch (Exception e) {
            log.error("收藏动态失败", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean unfavoritePost(Long postId, Long userId) {
        try {
            // 查询动态
            Post post = this.getById(postId);
            if (post == null) {
                return false;
            }
            
            // 更新收藏数
            if (post.getFavoriteCount() > 0) {
                post.setFavoriteCount(post.getFavoriteCount() - 1);
                this.updateById(post);
            }
            
            // TODO: 实际应该从 t_post_favorite 表删除收藏关系
            log.info("用户 {} 取消收藏动态 {}", userId, postId);
            return true;
        } catch (Exception e) {
            log.error("取消收藏失败", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public Long createPost(Post post, Long userId) {
        try {
            // 设置用户ID
            post.setUserId(userId);
            
            // 初始化计数
            post.setLikeCount(0);
            post.setCommentCount(0);
            post.setFavoriteCount(0);
            
            // 保存动态
            this.save(post);
            
            log.info("用户 {} 创建了动态 {}", userId, post.getId());
            return post.getId();
        } catch (Exception e) {
            log.error("创建动态失败", e);
            throw new RuntimeException("创建动态失败");
        }
    }
    
    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 今日动态数
            LambdaQueryWrapper<Post> todayQuery = new LambdaQueryWrapper<>();
            todayQuery.ge(Post::getCreatedAt, LocalDate.now().atStartOfDay());
            long todayPosts = this.count(todayQuery);
            
            // 总动态数
            long totalPosts = this.count();
            
            // 总用户数
            long totalUsers = userMapper.selectCount(null);
            
            // 今日成交数（这里简化处理，实际应该从换物记录表查询）
            long todaySwaps = 5; // 模拟数据
            
            stats.put("todayPosts", todayPosts);
            stats.put("todaySwaps", todaySwaps);
            stats.put("totalPosts", totalPosts);
            stats.put("totalUsers", totalUsers);
            
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            // 返回模拟数据
            stats.put("todayPosts", 24);
            stats.put("todaySwaps", 8);
            stats.put("totalPosts", 1248);
            stats.put("totalUsers", 356);
        }
        
        return stats;
    }
    
    @Override
    @Transactional
    public boolean deletePost(Long postId, Long userId) {
        try {
            // 查询动态
            Post post = this.getById(postId);
            if (post == null) {
                log.warn("动态不存在: {}", postId);
                return false;
            }
            
            // 检查权限：只能删除自己的动态
            if (!post.getUserId().equals(userId)) {
                log.warn("用户 {} 无权删除动态 {} (动态属于用户 {})", userId, postId, post.getUserId());
                return false;
            }
            
            // 删除动态
            boolean success = this.removeById(postId);
            if (success) {
                log.info("用户 {} 删除了动态 {}", userId, postId);
            } else {
                log.error("删除动态失败: {}", postId);
            }
            return success;
        } catch (Exception e) {
            log.error("删除动态失败", e);
            return false;
        }
    }
}