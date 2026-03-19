package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态举报实体 — 对应表 t_post_report
 */
@Data
@TableName("t_post_report")
public class PostReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报者用户ID */
    private Long reporterId;

    /** 被举报的动态ID */
    private Long postId;

    /** 举报原因类型：1-违法违规 2-色情低俗 3-虚假信息 4-侮辱谩骂 5-广告骚扰 6-其他 */
    private Integer reason;

    /** 补充说明 */
    private String description;

    /** 处理状态：0-待审核 1-已处理(内容下架) 2-已驳回(内容正常) */
    private Integer status;

    /** 管理员审核备注 */
    private String remark;

    /** 审核员ID */
    private Long reviewedBy;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
