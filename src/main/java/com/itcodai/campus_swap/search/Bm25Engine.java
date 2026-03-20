package com.itcodai.campus_swap.search;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BM25 相关性评分引擎
 * <p>
 * BM25 公式：
 * score(D, Q) = Σ IDF(qi) * (tf(qi,D) * (k1+1)) / (tf(qi,D) + k1*(1-b+b*|D|/avgdl))
 * <p>
 * 参数说明：
 * - k1：词频饱和参数，通常取 1.2~2.0，控制词频对得分的影响上限
 * - b：字段长度归一化参数，通常取 0.75，越大越倾向于惩罚长文档
 * - IDF：逆文档频率，log((N-df+0.5)/(df+0.5)+1)，df 越小的词权重越高
 *
 * @param <T> 文档类型
 */
public class Bm25Engine<T> {

    /** 词频饱和参数 */
    private static final double K1 = 1.5;

    /** 长度归一化参数 */
    private static final double B = 0.75;

    /** 文档列表 */
    private final List<T> documents;

    /** 每篇文档的词项列表 */
    private final List<List<String>> docTokens;

    /** 每篇文档的词频表：docIndex -> (term -> tf) */
    private final List<Map<String, Integer>> termFreqs;

    /** 文档频率表：term -> 出现该词的文档数 */
    private final Map<String, Integer> docFreq;

    /** 文档总数 */
    private final int N;

    /** 平均文档长度 */
    private final double avgDocLen;

    /**
     * 构造 BM25 引擎，传入文档集合和文本提取函数
     *
     * @param documents   文档列表
     * @param textExtractor 从文档中提取用于索引的文本（标题+描述+标签拼接）
     */
    public Bm25Engine(List<T> documents, java.util.function.Function<T, String> textExtractor) {
        this.documents = documents;
        this.N = documents.size();
        this.docTokens = new ArrayList<>(N);
        this.termFreqs = new ArrayList<>(N);
        this.docFreq = new HashMap<>();

        long totalLen = 0;

        // 建立倒排索引
        for (T doc : documents) {
            String text = textExtractor.apply(doc);
            List<String> tokens = SimpleTokenizer.tokenize(text);
            docTokens.add(tokens);
            totalLen += tokens.size();

            // 统计词频
            Map<String, Integer> tf = new HashMap<>();
            for (String token : tokens) {
                tf.merge(token, 1, Integer::sum);
            }
            termFreqs.add(tf);

            // 统计文档频率（每个词在多少文档中出现）
            tf.keySet().forEach(term -> docFreq.merge(term, 1, Integer::sum));
        }

        this.avgDocLen = N == 0 ? 1.0 : (double) totalLen / N;
    }

    /**
     * 对查询文本进行 BM25 评分，返回按得分降序排列的文档列表
     *
     * @param query   查询文本
     * @param topN    返回前 N 条
     * @return 按相关性降序排列的文档列表
     */
    public List<T> search(String query, int topN) {
        if (query == null || query.isEmpty() || N == 0) {
            return new ArrayList<>(documents.subList(0, Math.min(topN, documents.size())));
        }

        List<String> queryTokens = SimpleTokenizer.tokenize(query);
        if (queryTokens.isEmpty()) {
            return new ArrayList<>(documents.subList(0, Math.min(topN, documents.size())));
        }

        // 计算每篇文档的 BM25 得分
        double[] scores = new double[N];
        for (String term : queryTokens) {
            double idf = calcIdf(term);
            for (int i = 0; i < N; i++) {
                int tf = termFreqs.get(i).getOrDefault(term, 0);
                if (tf == 0) continue;
                int docLen = docTokens.get(i).size();
                double tfNorm = (tf * (K1 + 1))
                        / (tf + K1 * (1 - B + B * docLen / avgDocLen));
                scores[i] += idf * tfNorm;
            }
        }

        // 按得分降序排序，取前 topN
        Integer[] indices = new Integer[N];
        for (int i = 0; i < N; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Double.compare(scores[b], scores[a]));

        List<T> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, N); i++) {
            if (scores[indices[i]] > 0) {
                result.add(documents.get(indices[i]));
            }
        }
        return result;
    }

    /**
     * 对文档列表按 BM25 得分重新排序（用于已过滤后的子集排序）
     *
     * @param query     查询文本
     * @param subset    待排序的文档子集（必须是构造时传入文档的子集）
     * @return 按相关性降序排列的子集
     */
    public List<T> rank(String query, List<T> subset) {
        if (query == null || query.isEmpty()) return subset;
        List<String> queryTokens = SimpleTokenizer.tokenize(query);
        if (queryTokens.isEmpty()) return subset;

        // 建立文档到索引的映射
        Map<T, Integer> docIndex = new IdentityHashMap<>();
        for (int i = 0; i < documents.size(); i++) {
            docIndex.put(documents.get(i), i);
        }

        return subset.stream()
                .filter(docIndex::containsKey)
                .sorted((a, b) -> {
                    double sa = bm25Score(queryTokens, docIndex.get(a));
                    double sb = bm25Score(queryTokens, docIndex.get(b));
                    return Double.compare(sb, sa);
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算单篇文档对查询词列表的 BM25 得分
     */
    private double bm25Score(List<String> queryTokens, int docIdx) {
        double score = 0.0;
        int docLen = docTokens.get(docIdx).size();
        for (String term : queryTokens) {
            int tf = termFreqs.get(docIdx).getOrDefault(term, 0);
            if (tf == 0) continue;
            double idf = calcIdf(term);
            double tfNorm = (tf * (K1 + 1))
                    / (tf + K1 * (1 - B + B * docLen / avgDocLen));
            score += idf * tfNorm;
        }
        return score;
    }

    /**
     * 计算 IDF（逆文档频率）
     * 公式：log((N - df + 0.5) / (df + 0.5) + 1)
     */
    private double calcIdf(String term) {
        int df = docFreq.getOrDefault(term, 0);
        return Math.log((N - df + 0.5) / (df + 0.5) + 1);
    }
}
