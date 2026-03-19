package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcodai.campus_swap.entity.Comment;
import com.itcodai.campus_swap.entity.Post;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.CommentMapper;
import com.itcodai.campus_swap.mapper.PostMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.CommentService;
import com.itcodai.campus_swap.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Long addComment(Long postId, Long userId, String content, Long parentId) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        save(comment);

        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("comment_count = comment_count + 1")
                .setSql("hot_score = like_count * 3 + (comment_count + 1) * 2 + favorite_count * 4 + share_count * 1 + view_count * 0.1"));

        // 通知帖主
        try {
            Post post = postMapper.selectById(postId);
            if (post != null) {
                String preview = content != null && content.length() > 50 ? content.substring(0, 50) : content;
                notificationService.send(post.getUserId(), userId, "COMMENT", postId, preview);
            }
        } catch (Exception e) {
            log.error("发送评论通知失败 postId={} userId={}", postId, userId, e);
        }

        return comment.getId();
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        Comment comment = getById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            return false;
        }
        boolean removed = removeById(commentId);
        if (removed) {
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, comment.getPostId())
                    .setSql("comment_count = GREATEST(comment_count - 1, 0)")
                    .setSql("hot_score = like_count * 3 + GREATEST(comment_count - 1, 0) * 2 + favorite_count * 4 + share_count * 1 + view_count * 0.1"));
        }
        return removed;
    }

    @Override
    public List<Map<String, Object>> getComments(Long postId) {
        List<Comment> comments = list(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .isNull(Comment::getParentId)
                        .orderByAsc(Comment::getCreatedAt)
        );

        return comments.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("content", c.getContent());
            map.put("createdAt", c.getCreatedAt());
            map.put("userId", c.getUserId());

            User user = userMapper.selectById(c.getUserId());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getNickname());
                userInfo.put("avatar", user.getAvatar());
                map.put("user", userInfo);
            }

            // 子评论（回复）
            List<Comment> replies = list(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getParentId, c.getId())
                            .orderByAsc(Comment::getCreatedAt)
            );
            List<Map<String, Object>> replyList = replies.stream().map(r -> {
                Map<String, Object> rm = new HashMap<>();
                rm.put("id", r.getId());
                rm.put("content", r.getContent());
                rm.put("createdAt", r.getCreatedAt());
                rm.put("userId", r.getUserId());
                User ru = userMapper.selectById(r.getUserId());
                if (ru != null) {
                    Map<String, Object> rUserInfo = new HashMap<>();
                    rUserInfo.put("id", ru.getId());
                    rUserInfo.put("username", ru.getNickname());
                    rUserInfo.put("avatar", ru.getAvatar());
                    rm.put("user", rUserInfo);
                }
                return rm;
            }).collect(Collectors.toList());
            map.put("replies", replyList);

            return map;
        }).collect(Collectors.toList());
    }
}
