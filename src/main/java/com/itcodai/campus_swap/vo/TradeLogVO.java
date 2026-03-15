package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易状态变更日志 VO
 */
@Data
public class TradeLogVO {

    private Long id;
    private Long tradeId;

    /** 变更前状态 code */
    private String fromStatus;
    /** 变更前状态中文描述 */
    private String fromStatusDesc;

    /** 变更后状态 code */
    private String toStatus;
    /** 变更后状态中文描述 */
    private String toStatusDesc;

    /** 触发方式：0=手动操作  1=系统自动 */
    private Integer triggerType;
    /** 触发方式描述 */
    private String triggerTypeDesc;

    private Long operatorId;
    private String operatorNickname;

    private String remark;
    private LocalDateTime createdAt;
}
