package com.itcodai.campus_swap.search;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻量级中文分词器
 * 策略：
 * 1. 英文/数字按空格和标点切分
 * 2. 中文按 2-gram 滑动窗口切分（bigram），兼顾召回率
 * 3. 过滤停用词
 */
public class SimpleTokenizer {

    /** 中文停用词 */
    private static final java.util.Set<String> STOP_WORDS = java.util.Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都",
            "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你",
            "会", "着", "没有", "看", "好", "自己", "这", "那", "里", "来",
            "他", "她", "它", "们", "个", "这个", "那个", "什么", "怎么"
    );

    /**
     * 对文本进行分词
     *
     * @param text 输入文本
     * @return 词项列表
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> tokens = new ArrayList<>();
        text = text.toLowerCase().trim();

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isChinese(c)) {
                // 先把英文缓冲区 flush
                if (buf.length() > 0) {
                    addEnglishTokens(buf.toString(), tokens);
                    buf.setLength(0);
                }
                // 中文 unigram
                String uni = String.valueOf(c);
                if (!STOP_WORDS.contains(uni)) {
                    tokens.add(uni);
                }
                // 中文 bigram（与下一个字组合）
                if (i + 1 < text.length() && isChinese(text.charAt(i + 1))) {
                    String bi = text.substring(i, i + 2);
                    if (!STOP_WORDS.contains(bi)) {
                        tokens.add(bi);
                    }
                }
            } else if (Character.isLetterOrDigit(c)) {
                buf.append(c);
            } else {
                if (buf.length() > 0) {
                    addEnglishTokens(buf.toString(), tokens);
                    buf.setLength(0);
                }
            }
        }
        if (buf.length() > 0) {
            addEnglishTokens(buf.toString(), tokens);
        }
        return tokens;
    }

    private static void addEnglishTokens(String word, List<String> tokens) {
        if (word.length() >= 1 && !STOP_WORDS.contains(word)) {
            tokens.add(word);
        }
    }

    private static boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
    }
}
