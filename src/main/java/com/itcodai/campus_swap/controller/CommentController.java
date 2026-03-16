package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 广场评论控制器
 * GET    /square/comments/{postId} - 获取评论列表
 * POST   /square/comment          - 发表评论
 * DELETE /square/comment/{id}     - 删除评论
 */
@Slf4j
@RestController
@RequestMapping({"/square", "/api/square"})
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/comments/{postId}")
    public Result<List<Map<String, Object>>> getComments(@PathVariable Long postId) {
        try {
            return Result.success(commentService.getComments(postId));
        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "获取评论列表失败");
        }
    }

    @PostMapping("/comment")
    public Result<Long> addComment(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
        }

        Object postIdObj = body.get("postId");
        Object contentObj = body.get("content");
        if (postIdObj == null || contentObj == null) {
            return Result.fail(ResultCode.BAD_REQUEST, "参数不完整");
        }

        String content = contentObj.toString().trim();
        if (!StringUtils.hasText(content)) {
            return Result.fail(ResultCode.BAD_REQUEST, "评论内容不能为空");
        }

        Long postId = Long.valueOf(postIdObj.toString());
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;

        try {
            Long commentId = commentService.addComment(postId, userId, content, parentId);
            return Result.success(commentId);
        } catch (Exception e) {
            log.error("发表评论失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "发表评论失败");
        }
    }

    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
        }

        try {
            boolean success = commentService.deleteComment(commentId, userId);
            return success ? Result.success() : Result.fail(ResultCode.BAD_REQUEST, "删除失败");
        } catch (Exception e) {
            log.error("删除评论失败", e);
            return Result.fail(ResultCode.INTERNAL_ERROR, "删除评论失败");
        }
    }

    private Long getUserId(HttpServletRequest request) {
        Object obj = request.getAttribute("userId");
        return obj instanceof Long ? (Long) obj : null;
    }
}
