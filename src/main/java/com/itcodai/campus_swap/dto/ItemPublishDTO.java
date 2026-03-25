package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发布商品请求 DTO
 */
@Data
public class ItemPublishDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.0", message = "价格不能为负数")
    private BigDecimal price;

    @NotBlank(message = "请选择分类")
    private String category;

    private String description;

    /** 图片 URL 列表（第一张自动作为封面） */
    private List<String> images;

    /** 标签列表，如 ["九成新","免议价"] */
    private List<String> tags;

    /** 是否在审核通过后同步发布广场动态 */
    private Boolean syncToSquare;
}
