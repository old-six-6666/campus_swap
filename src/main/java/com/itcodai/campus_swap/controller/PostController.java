package com.itcodai.campus_swap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.entity.Post;
import com.itcodai.campus_swap.entity.Trade;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.mapper.TradeMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.PostService;
import com.itcodai.campus_swap.vo.PageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
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
    private final TradeMapper tradeMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    public PostController(PostService postService,
                          TradeMapper tradeMapper,
                          ItemMapper itemMapper,
                          UserMapper userMapper) {
        this.postService = postService;
        this.tradeMapper = tradeMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
    }

    /**
     * 获取我的已完成换物记录（用于发布"换物成功"动态时选择）
     * GET /post/my-swaps
     */
    @GetMapping("/my-swaps")
    public Result<List<Map<String, Object>>> getMySwapRecords(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
        }
        try {
            List<Trade> trades = tradeMapper.selectList(
                    new LambdaQueryWrapper<Trade>()
                            .eq(Trade::getStatus, "COMPLETED")
                            .and(w -> w.eq(Trade::getInitiatorId, userId)
                                      .or().eq(Trade::getReceiverId, userId))
                            .orderByDesc(Trade::getCompletedAt)
            );
            List<Map<String, Object>> result = new ArrayList<>();
            for (Trade trade : trades) {
                boolean isInitiator = userId.equals(trade.getInitiatorId());
                Long myItemId = isInitiator ? trade.getInitiatorItemId() : trade.getReceiverItemId();
                Long partnerItemId = isInitiator ? trade.getReceiverItemId() : trade.getInitiatorItemId();
                Long partnerId = isInitiator ? trade.getReceiverId() : trade.getInitiatorId();

                Item myItem = myItemId != null ? itemMapper.selectById(myItemId) : null;
                Item partnerItem = partnerItemId != null ? itemMapper.selectById(partnerItemId) : null;
                User partner = partnerId != null ? userMapper.selectById(partnerId) : null;

                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", trade.getId());
                map.put("itemATitle", myItem != null ? myItem.getTitle() : "未知物品");
                map.put("itemBTitle", partnerItem != null ? partnerItem.getTitle() : "未知物品");
                map.put("partnerName", partner != null ? partner.getNickname() : "未知用户");
                map.put("completedAt", trade.getCompletedAt());
                result.add(map);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取换物记录失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "获取换物记录失败");
        }
    }

    /**
     * 获取单个动态详情，同时异步记录浏览量
     * GET /post/{postId}
     */
    @GetMapping("/{postId:\\d+}")
    public Result<Map<String, Object>> getPostDetail(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long currentUserId = getUserIdFromRequest(request);
            Map<String, Object> detail = postService.getPostDetail(postId, currentUserId);
            if (detail == null) {
                return Result.fail(ResultCode.NOT_FOUND, "动态不存在");
            }
            // 异步 +1 浏览量，不阻塞响应
            new Thread(() -> postService.incrementView(postId)).start();
            return Result.success(detail);
        } catch (Exception e) {
            log.error("获取动态详情失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "获取动态详情失败");
        }
    }

    /**
     * 分享动态，share_count +1
     * POST /post/{postId}/share
     */
    @PostMapping("/{postId:\\d+}/share")
    public Result<Void> sharePost(@PathVariable Long postId) {
        postService.sharePost(postId);
        return Result.success();
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
            @RequestParam(required = false) String tag,
            HttpServletRequest request) {

        try {
            // 未登录用户也可浏览，isLiked/isFavorited 均返回 false
            Long currentUserId = getUserIdFromRequest(request);
            PageVO<Map<String, Object>> pageVO = postService.getPosts(page, size, sort, keyword, tag, currentUserId);
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
    @PostMapping("/{postId:\\d+}/like")
    public Result<Void> likePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            postService.likePost(postId, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("点赞动态失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "点赞失败");
        }
    }

    /**
     * 取消点赞
     * DELETE /api/post/{postId}/like
     */
    @DeleteMapping("/{postId:\\d+}/like")
    public Result<Void> unlikePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            postService.unlikePost(postId, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消点赞失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "取消点赞失败");
        }
    }
    
    /**
     * 收藏动态
     * POST /api/post/{postId}/favorite
     */
    @PostMapping("/{postId:\\d+}/favorite")
    public Result<Void> favoritePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            postService.favoritePost(postId, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("收藏动态失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "收藏失败");
        }
    }

    /**
     * 取消收藏
     * DELETE /api/post/{postId}/favorite
     */
    @DeleteMapping("/{postId:\\d+}/favorite")
    public Result<Void> unfavoritePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            postService.unfavoritePost(postId, userId);
            return Result.success();
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

            // 将前端传来的 imageList 序列化为 JSON 字符串存入 images 字段
            if (post.getImageList() != null && !post.getImageList().isEmpty()) {
                post.setImages(new ObjectMapper().writeValueAsString(post.getImageList()));
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
     * 编辑动态（只能编辑自己的动态，content 和 images 均可更新）
     * PUT /api/post/{postId}
     */
    @PutMapping("/{postId:\\d+}")
    public Result<Void> updatePost(@PathVariable Long postId,
                                   @RequestBody Post post,
                                   HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }

            if (post.getContent() != null && post.getContent().trim().isEmpty()) {
                return Result.fail(ResultCode.BAD_REQUEST, "动态内容不能为空");
            }

            // 将前端传来的 imageList 序列化为 JSON 字符串
            if (post.getImageList() != null) {
                post.setImages(new ObjectMapper().writeValueAsString(post.getImageList()));
            }

            boolean success = postService.updatePost(postId, post, userId);
            if (!success) {
                return Result.fail(ResultCode.BAD_REQUEST, "编辑失败，动态不存在或无权限");
            }
            return Result.success();
        } catch (Exception e) {
            log.error("编辑动态失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "编辑动态失败");
        }
    }

    /**
     * 删除动态（只能删除自己的动态）
     * DELETE /api/post/{postId}
     */
    @DeleteMapping("/{postId:\\d+}")
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
     * 获取当前用户收藏的动态列表
     * GET /api/post/favorites
     */
    @GetMapping("/favorites")
    public Result<Map<String, Object>> getMyFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
            }
            PageVO<Map<String, Object>> pageVO = postService.getFavoritePosts(userId, page, size);
            Map<String, Object> result = Map.of(
                "records", pageVO.getRecords(),
                "total", pageVO.getTotal(),
                "page", pageVO.getPage(),
                "size", pageVO.getSize()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "获取收藏列表失败");
        }
    }

    /**
     * 获取指定用户发布的动态列表（公开）
     * GET /api/post/user/{userId}
     */
    @GetMapping("/user/{userId:\\d+}")
    public Result<Map<String, Object>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long currentUserId = getUserIdFromRequest(request);
            PageVO<Map<String, Object>> pageVO = postService.getUserPosts(userId, page, size, currentUserId);
            Map<String, Object> result = Map.of(
                "records", pageVO.getRecords(),
                "total", pageVO.getTotal(),
                "page", pageVO.getPage(),
                "size", pageVO.getSize()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户动态列表失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "获取动态列表失败");
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