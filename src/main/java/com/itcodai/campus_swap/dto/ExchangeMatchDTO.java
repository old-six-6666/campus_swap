package com.itcodai.campus_swap.dto;

import lombok.Data;

import java.util.List;

/**
 * 以物换物匹配请求 DTO
 */
@Data
public class ExchangeMatchDTO {

    /** 当前物品ID */
    private Long itemId;

    /** 当前物品分类 */
    private String category;

    /** 当前物品期望换取的分类列表 */
    private List<String> expectCategory;

    /** 当前物品描述关键词（用于BM25相似度，可为空） */
    private String descriptionKeyword;

    /** 城市过滤（可为空） */
    private String city;

    /** 当前位置纬度（可为空） */
    private Double lat;

    /** 当前位置经度（可为空） */
    private Double lon;

    /** 最小新旧程度（1-5，可为空） */
    private Integer minNewDegree;

    /** 最大新旧程度（1-5，可为空） */
    private Integer maxNewDegree;

    /** 页码，默认1 */
    private Integer pageNum = 1;

    /** 每页条数，默认20 */
    private Integer pageSize = 20;
}
