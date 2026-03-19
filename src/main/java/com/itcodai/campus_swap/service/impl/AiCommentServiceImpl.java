package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcodai.campus_swap.entity.Comment;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.entity.Post;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.mapper.PostMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.AiCommentService;
import com.itcodai.campus_swap.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiCommentServiceImpl implements AiCommentService {

    @Lazy
    @Autowired
    private CommentService commentService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.api.model}")
    private String model;

    @Value("${deepseek.ai.user-id}")
    private Long aiUserId;

    @Override
    @Async
    public void triggerAiReply(Long postId, Long parentCommentId, String question) {
        log.info("AI 问一问触发：postId={}, parentCommentId={}, question={}", postId, parentCommentId, question);
        try {
            String dataContext = buildDataContext(question);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildSystemPrompt(dataContext)));
            messages.add(Map.of("role", "user", "content", question));

            String answer = callDeepSeek(messages);
            commentService.addComment(postId, aiUserId, answer, parentCommentId);
            log.info("AI 回复写入成功：postId={}, parentCommentId={}", postId, parentCommentId);
        } catch (Exception e) {
            log.error("AI 回复失败：postId={}, parentCommentId={}", postId, parentCommentId, e);
        }
    }

    @Override
    @Async
    public void triggerAiContinue(Long postId, Long rootCommentId, Long newReplyId) {
        log.info("AI 续对话触发：postId={}, rootCommentId={}, newReplyId={}", postId, rootCommentId, newReplyId);
        try {
            // 拉取一级评论本身（对话起点）
            Comment root = commentService.getById(rootCommentId);

            // 拉取该线程下所有子评论（对话历史）
            List<Comment> replies = commentService.list(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getParentId, rootCommentId)
                            .orderByAsc(Comment::getCreatedAt)
            );

            // 用最新一条用户消息做数据上下文检索
            Comment newReply = commentService.getById(newReplyId);
            String latestQuestion = newReply != null ? newReply.getContent() : "";
            String dataContext = buildDataContext(latestQuestion);

            // 构建多轮对话 messages
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildSystemPrompt(dataContext)));

            // 一级评论（用户第一问，去掉 @问一问 前缀）
            if (root != null) {
                String firstQuestion = root.getContent();
                if (firstQuestion.startsWith("@问一问")) {
                    firstQuestion = firstQuestion.substring("@问一问".length()).trim();
                }
                messages.add(Map.of("role", "user", "content", firstQuestion));
            }

            // 历史回复按时间顺序加入
            for (Comment reply : replies) {
                if (reply.getId().equals(newReplyId)) continue;
                String role = reply.getUserId().equals(aiUserId) ? "assistant" : "user";
                messages.add(Map.of("role", role, "content", reply.getContent()));
            }

            // 最新用户消息
            if (newReply != null) {
                messages.add(Map.of("role", "user", "content", newReply.getContent()));
            }

            String answer = callDeepSeek(messages);
            commentService.addComment(postId, aiUserId, answer, rootCommentId);
            log.info("AI 续对话回复写入成功：postId={}, rootCommentId={}", postId, rootCommentId);
        } catch (Exception e) {
            log.error("AI 续对话回复失败：postId={}, rootCommentId={}", postId, rootCommentId, e);
        }
    }

    /**
     * 根据用户问题查询数据库，构建数据上下文摘要
     */
    private String buildDataContext(String question) {
        if (question == null || question.isBlank()) return "";

        StringBuilder ctx = new StringBuilder();
        String q = question.toLowerCase();

        // 查询动态/帖子相关
        if (containsAny(q, "动态", "帖子", "广场", "发布", "分享", "post")) {
            try {
                List<Post> posts = postMapper.selectList(
                        new LambdaQueryWrapper<Post>()
                                .orderByDesc(Post::getCreatedAt)
                                .last("LIMIT 20")
                );
                if (!posts.isEmpty()) {
                    ctx.append("【平台最新动态（最多20条）】\n");
                    for (Post post : posts) {
                        User user = userMapper.selectById(post.getUserId());
                        String nickname = user != null ? user.getNickname() : "匿名用户";
                        String typeStr = postTypeStr(post.getType());
                        String content = post.getContent() != null && post.getContent().length() > 80
                                ? post.getContent().substring(0, 80) + "..."
                                : post.getContent();
                        ctx.append(String.format("- [%s] %s 发布：%s（点赞%d 评论%d）\n",
                                typeStr, nickname, content,
                                post.getLikeCount(), post.getCommentCount()));
                    }
                    ctx.append(String.format("共 %d 条动态。\n\n", postMapper.selectCount(null)));
                }
            } catch (Exception e) {
                log.warn("查询动态数据失败", e);
            }
        }

        // 查询物品相关
        if (containsAny(q, "物品", "商品", "闲置", "出售", "换", "二手", "item", "东西")) {
            try {
                List<Item> items = itemMapper.selectList(
                        new LambdaQueryWrapper<Item>()
                                .eq(Item::getAuditStatus, 1)
                                .orderByDesc(Item::getCreatedAt)
                                .last("LIMIT 20")
                );
                if (!items.isEmpty()) {
                    ctx.append("【平台物品列表（最多20件）】\n");
                    // 按分类聚合
                    Map<String, List<Item>> byCategory = items.stream()
                            .collect(Collectors.groupingBy(i -> i.getCategory() != null ? i.getCategory() : "其他"));
                    for (Map.Entry<String, List<Item>> entry : byCategory.entrySet()) {
                        ctx.append(String.format("  %s类：", entry.getKey()));
                        ctx.append(entry.getValue().stream()
                                .map(i -> String.format("%s(¥%.0f)", i.getTitle(), i.getPrice()))
                                .collect(Collectors.joining("、")));
                        ctx.append("\n");
                    }
                    long total = itemMapper.selectCount(
                            new LambdaQueryWrapper<Item>().eq(Item::getAuditStatus, 1));
                    ctx.append(String.format("共 %d 件物品。\n\n", total));
                } else {
                    ctx.append("【平台物品】目前暂无已上架物品。\n\n");
                }
            } catch (Exception e) {
                log.warn("查询物品数据失败", e);
            }
        }

        // 查询统计信息
        if (containsAny(q, "统计", "数量", "多少", "几个", "几条", "总共", "概括", "汇总", "概况", "情况")) {
            try {
                long totalPosts = postMapper.selectCount(null);
                long totalItems = itemMapper.selectCount(
                        new LambdaQueryWrapper<Item>().eq(Item::getAuditStatus, 1));
                long totalUsers = userMapper.selectCount(
                        new LambdaQueryWrapper<User>().ne(User::getId, aiUserId));
                ctx.append("【平台数据概况】\n");
                ctx.append(String.format("- 总动态数：%d 条\n", totalPosts));
                ctx.append(String.format("- 在架物品数：%d 件\n", totalItems));
                ctx.append(String.format("- 注册用户数：%d 人\n\n", totalUsers));
            } catch (Exception e) {
                log.warn("查询统计数据失败", e);
            }
        }

        return ctx.toString();
    }

    private String buildSystemPrompt(String dataContext) {
        String base = "你是校园换物平台的AI助手「问一问」，请简洁、友好地回答用户的问题。回答控制在300字以内。";
        if (dataContext.isBlank()) {
            return base;
        }
        return base + "\n\n以下是平台的真实数据，请基于这些数据回答用户问题：\n" + dataContext;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String postTypeStr(Integer type) {
        if (type == null) return "动态";
        return switch (type) {
            case 1 -> "发布物品";
            case 2 -> "换物成功";
            case 3 -> "分享动态";
            case 4 -> "求换动态";
            default -> "动态";
        };
    }

    private String callDeepSeek(List<Map<String, Object>> messages) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        String requestJson = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        String response = restTemplate.postForObject(apiUrl, entity, String.class);
        JsonNode root = objectMapper.readTree(response);
        return root.path("choices").get(0).path("message").path("content").asText();
    }
}
