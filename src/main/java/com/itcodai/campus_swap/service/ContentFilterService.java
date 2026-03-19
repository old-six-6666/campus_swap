package com.itcodai.campus_swap.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内容安全过滤服务（敏感词检测）
 */
@Service
public class ContentFilterService {

    // 敏感词列表（可根据需要扩充）
    private static final List<String> SENSITIVE_WORDS = List.of(
            // 暴力
            "暴力", "杀人", "砍人", "打死", "枪击", "爆炸", "炸弹", "恐怖袭击", "持刀", "伤害",
            // 色情
            "色情", "黄片", "裸体", "性交", "卖淫", "嫖娼", "援交",
            // 辱骂
            "傻逼", "操你", "去死", "废物", "滚", "贱人", "垃圾人", "混蛋", "王八蛋", "狗杂种",
            // 违禁品
            "毒品", "冰毒", "海洛因", "大麻", "可卡因", "卖毒", "买毒",
            // 政治敏感（仅列举常见违规表述，不展开）
            "推翻政府", "颠覆国家"
    );

    /**
     * 检测文本是否含有敏感词
     *
     * @param text 待检测文本
     * @return true=含有敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
