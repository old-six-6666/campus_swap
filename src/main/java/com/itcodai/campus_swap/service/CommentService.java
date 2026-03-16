package com.itcodai.campus_swap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcodai.campus_swap.entity.Comment;

import java.util.List;
import java.util.Map;

public interface CommentService extends IService<Comment> {

    Long addComment(Long postId, Long userId, String content, Long parentId);

    boolean deleteComment(Long commentId, Long userId);

    List<Map<String, Object>> getComments(Long postId);
}
