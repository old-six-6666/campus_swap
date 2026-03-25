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
            String dataContext = buildDataContext(question, postId);

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
            String dataContext = buildDataContext(latestQuestion, postId);

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
    private String buildDataContext(String question, Long postId) {
        if (question == null || question.isBlank()) return "";

        StringBuilder ctx = new StringBuilder();
        String q = question.toLowerCase();

        // ---- 1. 比价分支（优先级最高）----
        if (containsAny(q, "比价", "对比", "哪个", "哪家", "便宜", "价格", "多少钱", "最低", "贵", "值不值")) {
            try {
                String targetCategory = null;
                // 尝试从当前帖子关联商品中获取分类
                if (postId != null) {
                    Post post = postMapper.selectById(postId);
                    if (post != null && post.getItemId() != null) {
                        Item linkedItem = itemMapper.selectById(post.getItemId());
                        if (linkedItem != null) {
                            targetCategory = linkedItem.getCategory();
                        }
                    }
                }

                if (targetCategory != null) {
                    // 同分类比价
                    List<Item> sameCategory = itemMapper.selectList(
                            new LambdaQueryWrapper<Item>()
                                    .eq(Item::getAuditStatus, 1)
                                    .eq(Item::getStatus, 0)
                                    .eq(Item::getCategory, targetCategory)
                                    .orderByAsc(Item::getPrice)
                                    .last("LIMIT 30")
                    );
                    ctx.append(String.format("【同类商品比价 — %s类（共%d件在售）】\n从低到高：\n",
                            targetCategory, sameCategory.size()));
                    for (Item i : sameCategory) {
                        String desc = i.getDescription() != null && i.getDescription().length() > 60
                                ? i.getDescription().substring(0, 60) + "..."
                                : i.getDescription();
                        ctx.append(String.format("- %s：¥%.2f — %s\n",
                                i.getTitle(), i.getPrice().doubleValue(),
                                desc != null ? desc : "无描述"));
                    }
                    ctx.append("提示：以上均为挂牌价，实际可议价。\n\n");
                } else {
                    // 无关联商品，展示各分类价格区间
                    List<Item> allItems = itemMapper.selectList(
                            new LambdaQueryWrapper<Item>()
                                    .eq(Item::getAuditStatus, 1)
                                    .eq(Item::getStatus, 0)
                                    .orderByAsc(Item::getPrice)
                    );
                    if (!allItems.isEmpty()) {
                        Map<String, List<Item>> byCategory = allItems.stream()
                                .collect(Collectors.groupingBy(i -> i.getCategory() != null ? i.getCategory() : "其他"));
                        ctx.append("【各类商品价格区间】\n");
                        for (Map.Entry<String, List<Item>> entry : byCategory.entrySet()) {
                            List<Item> list = entry.getValue();
                            double min = list.get(0).getPrice().doubleValue();
                            double max = list.get(list.size() - 1).getPrice().doubleValue();
                            ctx.append(String.format("- %s类：¥%.2f ~ ¥%.2f（共%d件）\n",
                                    entry.getKey(), min, max, list.size()));
                        }
                        ctx.append("\n");
                    }
                }
            } catch (Exception e) {
                log.warn("查询比价数据失败", e);
            }
        }

        // ---- 2. 评论摘要分支 ----
        if (postId != null && containsAny(q, "评论", "大家说", "反馈", "意见", "网友", "看法", "怎么说", "回复")) {
            try {
                List<Comment> rawComments = commentService.list(
                        new LambdaQueryWrapper<Comment>()
                                .eq(Comment::getPostId, postId)
                                .isNull(Comment::getParentId)
                                .ne(Comment::getUserId, aiUserId)
                                .orderByDesc(Comment::getCreatedAt)
                                .last("LIMIT 20")
                );
                List<Comment> comments = rawComments.stream()
                        .filter(c -> !isTradeComment(c.getContent()))
                        .limit(15)
                        .collect(Collectors.toList());
                if (!comments.isEmpty()) {
                    ctx.append(String.format("【本帖评论摘要（最多15条）】\n"));
                    for (Comment c : comments) {
                        User u = userMapper.selectById(c.getUserId());
                        String nickname = u != null ? u.getNickname() : "匿名用户";
                        String content = c.getContent() != null && c.getContent().length() > 80
                                ? c.getContent().substring(0, 80) + "..."
                                : c.getContent();
                        ctx.append(String.format("- %s：%s\n", nickname, content));
                    }
                    ctx.append(String.format("共%d条用户评论。\n\n", comments.size()));
                }
            } catch (Exception e) {
                log.warn("查询评论数据失败", e);
            }
        }

        // ---- 3. 动态/帖子分支（原有）----
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

        // ---- 4. 商品列表分支（优化输出，含描述）----
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
                    Map<String, List<Item>> byCategory = items.stream()
                            .collect(Collectors.groupingBy(i -> i.getCategory() != null ? i.getCategory() : "其他"));
                    for (Map.Entry<String, List<Item>> entry : byCategory.entrySet()) {
                        ctx.append(String.format("  %s类：\n", entry.getKey()));
                        for (Item i : entry.getValue()) {
                            String desc = i.getDescription() != null && i.getDescription().length() > 50
                                    ? i.getDescription().substring(0, 50) + "..."
                                    : i.getDescription();
                            ctx.append(String.format("    - %s / ¥%.2f / %s\n",
                                    i.getTitle(), i.getPrice().doubleValue(),
                                    desc != null ? desc : "无描述"));
                        }
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

        // ---- 5. 统计分支（原有）----
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
        String base = "你是校园换物平台的AI助手「问一问」，请简洁、友好地回答用户的问题。" +
                "回答控制在300字以内。严禁透露任何用户的手机号、邮箱或其他私人联系方式。";
        if (dataContext.isBlank()) {
            return base;
        }
        return base + "\n\n以下是平台的真实数据，请基于这些数据回答用户问题：\n" + dataContext;
    }

    private boolean isTradeComment(String content) {
        if (content == null || content.isBlank()) return false;
        return containsAny(content, "交易", "换物", "已换", "成交", "付款",
                "发货", "收货", "快递", "运费", "到手", "已拍", "付定", "转账");
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
