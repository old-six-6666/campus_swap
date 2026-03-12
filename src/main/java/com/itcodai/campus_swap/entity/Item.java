package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品（闲置物）实体
 */
@Data
@TableName("t_item")
public class Item {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 价格 */
    private BigDecimal price;

    /** 分类：数码/书籍/服饰/生活用品/其他 */
    private String category;

    /** 详细描述 */
    private String description;

    /** 封面图片 URL */
    private String coverImage;

    /** 发布者 ID */
    private Long sellerId;

    /**
     * 状态：0-在售 1-已下架 2-已售出
     */
    private Integer status;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
