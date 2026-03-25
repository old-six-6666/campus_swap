package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易申诉实体
 */
@Data
@TableName("t_trade_appeal")
public class TradeAppeal {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联交易ID */
    private Long tradeId;

    /** 申诉人用户ID */
    private Long appellantId;

    /** 申诉内容 */
    private String content;

    /**
     * 处理状态：0=待处理  1=已处理  2=已驳回
     */
    private Integer status;

    /** 管理员处理备注 */
    private String remark;

    /** 处理管理员ID */
    private Long reviewedBy;

    /** 处理时间 */
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
