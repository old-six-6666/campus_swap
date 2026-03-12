package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

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

    /** 封面图 URL（可为空） */
    private String coverImage;
}
