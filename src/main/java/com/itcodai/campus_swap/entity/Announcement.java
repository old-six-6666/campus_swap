package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告实体
 * 对应表 t_announcement
 */
@Data
@TableName("t_announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 公告类型: 1-普通 2-重要 3-紧急 */
    private Integer type;

    /** 状态: 0-下线 1-上线 */
    private Integer status;

    /** 排序权重，越大越靠前 */
    private Integer sort;

    /** 创建人ID */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
