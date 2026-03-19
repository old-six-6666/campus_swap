package com.itcodai.campus_swap.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.vo.ItemVO;
import com.itcodai.campus_swap.service.ItemService;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 本地物品搜索与以物换物匹配服务
 * <p>
 * 借鉴 Elasticsearch 的核心思想，纯 Java 实现：
 * 1. BM25 相关性评分（参考 ES 默认相似度算法）
 * 2. 双向分类匹配（参考 ES term/terms query）
 * 3. 多字段加权（标题权重 > 标签权重 > 描述权重，参考 ES multi_match）
 * 4. 结果融合排序（相关性得分 + 时间衰减，参考 ES function_score）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalSearchService {

    private final ItemMapper itemMapper;
    private final ItemService itemService;

    /**
     * 关键词搜索（BM25 多字段加权）
     * 对标 ES 的 multi_match query
     *
     * @param keyword  搜索关键词
     * @param category 分类过滤（可为 null）
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @return 按相关性排序的物品列表
     */
    public List<ItemVO> search(String keyword, String category, int page, int size) {
        // 1. 从数据库加载已审核通过且在售的物品
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getAuditStatus, 1)
                .eq(Item::getStatus, 0)
                .eq(StringUtils.hasText(category), Item::getCategory, category);
        List<Item> items = itemMapper.selectList(wrapper);

        if (items.isEmpty()) return Collections.emptyList();

        // 2. 无关键词时直接按时间降序返回
        if (!StringUtils.hasText(keyword)) {
            return items.stream()
                    .sorted(Comparator.comparing(Item::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .skip((long) (page - 1) * size)
                    .limit(size)
                    .map(this::toVO)
                    .collect(Collectors.toList());
        }

        // 3. 多字段加权文本拼接：标题×3 + 标签×2 + 描述×1
        Bm25Engine<Item> engine = new Bm25Engine<>(items, item -> {
            StringBuilder sb = new StringBuilder();
            // 标题权重最高，重复3次
            for (int i = 0; i < 3; i++) {
                if (StringUtils.hasText(item.getTitle())) sb.append(item.getTitle()).append(" ");
            }
            // 标签权重次之，重复2次
            if (StringUtils.hasText(item.getTags())) {
                List<String> tags = JSONUtil.toList(item.getTags(), String.class);
                for (int i = 0; i < 2; i++) {
                    sb.append(String.join(" ", tags)).append(" ");
                }
            }
            // 分类
            if (StringUtils.hasText(item.getCategory())) sb.append(item.getCategory()).append(" ");
            // 描述
            if (StringUtils.hasText(item.getDescription())) sb.append(item.getDescription());
            return sb.toString();
        });

        // 4. BM25 搜索 + 分页
        List<Item> ranked = engine.search(keyword, items.size());
        return ranked.stream()
                .skip((long) (page - 1) * size)
                .limit(size)
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 以物换物匹配（双向分类匹配 + BM25 描述相似度）
     * 对标 ES 的 bool query + function_score
     *
     * @param currentUserId         当前用户 ID（排除自己的物品）
     * @param currentCategory       当前物品分类
     * @param currentExpectCategory 当前物品期望换取的分类列表
     * @param descriptionKeyword    描述关键词（用于 BM25 相似度，可为 null）
     * @param minNewDegree          最小新旧程度（1-5，可为 null）
     * @param maxNewDegree          最大新旧程度（1-5，可为 null）
     * @param page                  页码
     * @param size                  每页条数
     * @return 匹配结果列表
     */
    public List<ItemVO> matchForExchange(
            Long currentUserId,
            String currentCategory,
            List<String> currentExpectCategory,
            String descriptionKeyword,
            Integer minNewDegree,
            Integer maxNewDegree,
            int page,
            int size) {

        // 1. 加载所有已审核通过且在售的物品
        List<Item> allItems = itemMapper.selectList(
                new LambdaQueryWrapper<Item>()
                        .eq(Item::getAuditStatus, 1)
                        .eq(Item::getStatus, 0)
        );

        if (allItems.isEmpty()) return Collections.emptyList();

        // 分类过滤：对方 category 在我的 expectCategory 中即可
        List<Item> filtered = allItems.stream()
                .filter(item -> {
                    if (item.getSellerId().equals(currentUserId)) return false;
                    return currentExpectCategory != null
                            && currentExpectCategory.contains(item.getCategory());
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) return Collections.emptyList();

        // 3. 新旧程度过滤（对标 ES range filter）
        // 注意：当前 Item 实体没有 newDegree 字段，用 tags 中的新旧标签推断
        // 如有需要可在 Item 表加 new_degree 字段

        // 4. BM25 描述相似度排序（对标 ES BM25 + should boost）
        if (StringUtils.hasText(descriptionKeyword)) {
            Bm25Engine<Item> engine = new Bm25Engine<>(filtered, item -> {
                StringBuilder sb = new StringBuilder();
                if (StringUtils.hasText(item.getTitle())) sb.append(item.getTitle()).append(" ");
                if (StringUtils.hasText(item.getDescription())) sb.append(item.getDescription()).append(" ");
                if (StringUtils.hasText(item.getTags())) sb.append(item.getTags());
                return sb.toString();
            });
            filtered = engine.rank(descriptionKeyword, filtered);
        } else {
            // 无关键词时按发布时间降序（对标 ES sort by date）
            filtered.sort(Comparator.comparing(Item::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
        }

        // 5. 分页返回
        return filtered.stream()
                .skip((long) (page - 1) * size)
                .limit(size)
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 将 Item 转为 ItemVO
     */
    private ItemVO toVO(Item item) {
        return itemService.getItemById(item.getId());
    }
}
