package com.itcodai.campus_swap.service;

/**
 * AI 问一问服务：检测到 @问一问 触发词后异步调用 DeepSeek API，将回答以子评论形式写回
 */
public interface AiCommentService {

    /**
     * 异步触发 AI 首次回复（@问一问 触发词）
     *
     * @param postId          动态 ID
     * @param parentCommentId 触发评论的 ID（AI 回复将作为其子评论）
     * @param question        用户提问内容（已去除 @问一问 前缀）
     */
    void triggerAiReply(Long postId, Long parentCommentId, String question);

    /**
     * 异步触发 AI 续对话回复（用户回复了 AI 的评论）
     *
     * @param postId          动态 ID
     * @param rootCommentId   一级评论 ID（对话所在的线程根节点）
     * @param newReplyId      用户刚发的回复评论 ID
     */
    void triggerAiContinue(Long postId, Long rootCommentId, Long newReplyId);
}
