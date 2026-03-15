package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易单详情 VO（返回给前端）
 */
@Data
public class TradeVO {

    private Long id;
    private String tradeNo;

    // ===== 甲方信息 =====
    private Long initiatorId;
    private String initiatorNickname;
    private String initiatorAvatar;

    // ===== 乙方信息 =====
    private Long receiverId;
    private String receiverNickname;
    private String receiverAvatar;

    // ===== 甲方物品 =====
    private Long initiatorItemId;
    private String initiatorItemTitle;
    private String initiatorItemCoverImage;

    // ===== 乙方物品 =====
    private Long receiverItemId;
    private String receiverItemTitle;
    private String receiverItemCoverImage;

    // ===== 状态 =====
    /** 当前状态 code，见 TradeStatus 枚举 */
    private String status;
    /** 状态中文描述 */
    private String statusDesc;

    // ===== 审核 =====
    /** 审核模式：0=无需审核 1=平台审核 2=双方互审 */
    private Integer auditMode;
    /** 审核备注 */
    private String auditRemark;

    // ===== 发货 =====
    private Boolean initiatorDelivered;
    private Boolean receiverDelivered;
    private String initiatorLogistics;
    private String receiverLogistics;

    // ===== 收货 =====
    private Boolean initiatorConfirmedReceipt;
    private Boolean receiverConfirmedReceipt;

    // ===== 终止 =====
    private String terminateReason;

    // ===== 时间线 =====
    private LocalDateTime deliveryDeadline;
    private LocalDateTime receiptDeadline;
    private LocalDateTime matchedAt;
    private LocalDateTime auditPassedAt;
    private LocalDateTime waitingDeliveryAt;
    private LocalDateTime bothDeliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime terminatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 当前登录用户在本次交易中的角色：initiator / receiver
     * 由 Service 层根据请求用户ID注入，便于前端渲染差异化 UI
     */
    private String myRole;
}
