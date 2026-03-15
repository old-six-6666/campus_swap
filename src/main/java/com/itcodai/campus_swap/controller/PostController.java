package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.entity.Post;
import com.itcodai.campus_swap.service.PostService;
import com.itcodai.campus_swap.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 动态控制器
 * 处理广场动态相关请求
 */
@Slf4j
@RestController
@RequestMapping({"/api/post", "/post"})
public class PostController {
    
    private final PostService postService;
    
    public PostController(PostService postService) {
        this.postService = postService;
    }
    
    /**
     * 获取动态列表
     * GET /api/post/list
     */
    @GetMapping({"/list", "/api/post/list"})
    public Result<Map<String, Object>> getPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "recommend") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag) {
        
        try {
            PageVO<Map<String, Object>> pageVO = postService.getPosts(page, size, sort, keyword, tag);
            Map<String, Object> result = Map.of(
                "records", pageVO.getRecords(),
                "total", pageVO.getTotal(),
                "page", pageVO.getPage(),
                "size", pageVO.getSize()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取动态列表失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "获取动态列表失败");
        }
    }
    
    /**
     * 点赞动态
     * POST /api/post/{postId}/like
     */
    @PostMapping("/{postId}/like")
    public Result<Void> likePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            
            boolean success = postService.likePost(postId, userId);
            return success ? Result.success() : Result.fail(ResultCode.BAD_REQUEST, "点赞失败");
        } catch (Exception e) {
            log.error("点赞动态失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "点赞失败");
        }
    }
    
    /**
     * 取消点赞
     * DELETE /api/post/{postId}/like
     */
    @DeleteMapping("/{postId}/like")
    public Result<Void> unlikePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            
            boolean success = postService.unlikePost(postId, userId);
            return success ? Result.success() : Result.fail(ResultCode.BAD_REQUEST, "取消点赞失败");
        } catch (Exception e) {
            log.error("取消点赞失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "取消点赞失败");
        }
    }
    
    /**
     * 收藏动态
     * POST /api/post/{postId}/favorite
     */
    @PostMapping("/{postId}/favorite")
    public Result<Void> favoritePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            
            boolean success = postService.favoritePost(postId, userId);
            return success ? Result.success() : Result.fail(ResultCode.BAD_REQUEST, "收藏失败");
        } catch (Exception e) {
            log.error("收藏动态失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "收藏失败");
        }
    }
    
    /**
     * 取消收藏
     * DELETE /api/post/{postId}/favorite
     */
    @DeleteMapping("/{postId}/favorite")
    public Result<Void> unfavoritePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            
            boolean success = postService.unfavoritePost(postId, userId);
            return success ? Result.success() : Result.fail(ResultCode.BAD_REQUEST, "取消收藏失败");
        } catch (Exception e) {
            log.error("取消收藏失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "取消收藏失败");
        }
    }
    
    /**
     * 创建动态
     * POST /api/post/create
     */
    @PostMapping("/create")
    public Result<Long> createPost(@RequestBody Post post, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            
            // 验证动态类型
            if (post.getType() == null || post.getType() < 1 || post.getType() > 4) {
                return Result.fail(ResultCode.BAD_REQUEST, "动态类型无效");
            }
            
            // 验证内容
            if (post.getContent() == null || post.getContent().trim().isEmpty()) {
                return Result.fail(ResultCode.BAD_REQUEST, "动态内容不能为空");
            }
            
            Long postId = postService.createPost(post, userId);
            return Result.success(postId);
        } catch (Exception e) {
            log.error("创建动态失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "创建动态失败");
        }
    }
    
    /**
     * 获取动态统计数据
     * GET /api/post/stats
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = postService.getStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "获取统计数据失败");
        }
    }
    
    /**
     * 删除动态（只能删除自己的动态）
     * DELETE /api/post/{postId}
     */
    @DeleteMapping("/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            
            boolean success = postService.deletePost(postId, userId);
            return success ? Result.success() : Result.fail(ResultCode.BAD_REQUEST, "删除动态失败");
        } catch (Exception e) {
            log.error("删除动态失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "删除动态失败");
        }
    }
    
    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        return null;
    }
}