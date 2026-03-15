package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易状态变更日志
 * <p>
 * 每次状态流转（包含手动触发与系统自动触发）均写入一条日志，提供完整审计链路。
 */
@Data
@TableName("t_trade_log")
public class TradeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的交易ID */
    private Long tradeId;

    /** 变更前状态（首次创建时为 null） */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /**
     * 触发方式：0=手动操作  1=系统自动（定时任务超时触发或状态联动）
     */
    private Integer triggerType;

    /** 操作人用户ID，系统自动触发时为 null */
    private Long operatorId;

    /** 备注（驳回原因、取消原因、超时说明等） */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
