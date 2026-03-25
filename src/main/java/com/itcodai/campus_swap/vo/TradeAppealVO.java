package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易申诉 VO（返回给前端）
 */
@Data
public class TradeAppealVO {

    private Long id;

    private Long tradeId;
    /** 交易单号 */
    private String tradeNo;

    private Long appellantId;
    private String appellantNickname;
    private String appellantAvatar;

    /** 申诉内容 */
    private String content;

    /**
     * 处理状态：0=待处理  1=已处理  2=已驳回
     */
    private Integer status;
    /** 状态中文描述 */
    private String statusDesc;

    /** 管理员处理备注 */
    private String remark;

    private Long reviewedBy;
    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
