package com.itcodai.campus_swap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容安全过滤服务
 * 两级检测：本地敏感词（毫秒级） → AI 语义检测（兜底）
 */
@Slf4j
@Service
public class ContentFilterService {

    // 本地敏感词列表（快速拦截，零成本）
    private static final List<String> SENSITIVE_WORDS = List.of(
            // 暴力
            "暴力", "杀人", "砍人", "打死", "枪击", "爆炸", "炸弹", "恐怖袭击", "持刀", "伤害",
            // 色情
            "色情", "黄片", "裸体", "性交", "卖淫", "嫖娼", "援交",
            // 辱骂
            "傻逼", "操你", "去死", "废物", "贱人", "垃圾人", "混蛋", "王八蛋", "狗杂种",
            // 违禁品
            "毒品", "冰毒", "海洛因", "大麻", "可卡因", "卖毒", "买毒",
            // 政治违规
            "推翻政府", "颠覆国家"
    );

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.api.model}")
    private String model;

    /**
     * 两级检测：先本地敏感词，再 AI 语义
     *
     * @return true = 含违规内容，应拦截
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isBlank()) return false;

        // 第一级：本地关键词（快速）
        String lower = text.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                log.info("本地敏感词命中：[{}]", word);
                return true;
            }
        }

        // 第二级：AI 语义检测（兜底）
        return aiDetect(text);
    }

    /**
     * 调用 DeepSeek 做语义内容审核
     * Prompt 要求只回答 YES / NO，控制 token 消耗
     */
    private boolean aiDetect(String text) {
        try {
            String prompt = "你是内容审核员。判断以下文字是否含有暴力、色情、辱骂、违禁品交易、政治敏感等违规内容。" +
                    "只回答 YES 或 NO，不要任何解释。\n\n文字：" + text;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("stream", false);
            requestBody.put("max_tokens", 5);  // 只需要 YES/NO，节省 token

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            String response = restTemplate.postForObject(apiUrl, entity, String.class);
            JsonNode root = objectMapper.readTree(response);
            String answer = root.path("choices").get(0).path("message").path("content").asText().trim().toUpperCase();

            log.info("AI 内容审核结果：[{}] → {}", text.length() > 30 ? text.substring(0, 30) + "..." : text, answer);
            return answer.startsWith("YES");
        } catch (Exception e) {
            // AI 调用失败时放行，避免影响正常发布
            log.warn("AI 内容审核调用失败，默认放行", e);
            return false;
        }
    }
}
