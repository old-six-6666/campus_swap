package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易单实体（以物换物）
 * <p>
 * 乐观锁字段 version 用于防止并发状态变更冲突，需配合
 * {@link com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor} 使用。
 */
@Data
@TableName("t_trade")
public class Trade {

    /** 交易ID，主键自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务编号，唯一，格式：TRADE + yyyyMMddHHmmss + 6位随机数 */
    private String tradeNo;

    /** 甲方（发起方）用户ID */
    private Long initiatorId;

    /** 乙方（接受方）用户ID，PENDING_MATCH 阶段可为空 */
    private Long receiverId;

    /** 甲方提供的物品ID */
    private Long initiatorItemId;

    /** 乙方提供的物品ID，MATCHED 之后才有值 */
    private Long receiverItemId;

    /**
     * 当前状态，取值见 {@link com.itcodai.campus_swap.enums.TradeStatus}
     */
    private String status;

    /**
     * 审核模式：0=无需审核  1=平台审核  2=双方互审
     * 通过配置文件 trade.audit.mode 控制，创建时写入
     */
    private Integer auditMode;

    /** 审核人ID（平台审核时为管理员ID） */
    private Long auditBy;

    /** 审核备注 / 驳回原因 */
    private String auditRemark;

    /** 甲方是否已发货 */
    private Boolean initiatorDelivered;

    /** 乙方是否已发货 */
    private Boolean receiverDelivered;

    /** 甲方物流凭证（快递公司+单号 等 JSON 信息） */
    private String initiatorLogistics;

    /** 乙方物流凭证 */
    private String receiverLogistics;

    /** 甲方是否已确认收货 */
    private Boolean initiatorConfirmedReceipt;

    /** 乙方是否已确认收货 */
    private Boolean receiverConfirmedReceipt;

    /** 终止原因（TERMINATED 状态时填写） */
    private String terminateReason;

    /** 甲方是否已申请终止（等待对方确认） */
    private Boolean initiatorWantTerminate;

    /** 乙方是否已申请终止（等待对方确认） */
    private Boolean receiverWantTerminate;

    /** 发货超时小时数（默认 48，来自配置文件） */
    private Integer deliveryTimeoutHours;

    /** 收货确认超时小时数（默认 72，来自配置文件） */
    private Integer receiptTimeoutHours;

    /** 发货截止时间，进入 WAITING_DELIVERY 时计算写入 */
    private LocalDateTime deliveryDeadline;

    /** 收货确认截止时间，进入 WAITING_CONFIRM_RECEIPT 时计算写入 */
    private LocalDateTime receiptDeadline;

    /** 进入 MATCHED 状态的时间 */
    private LocalDateTime matchedAt;

    /** 审核通过时间 */
    private LocalDateTime auditPassedAt;

    /** 进入 WAITING_DELIVERY 状态的时间 */
    private LocalDateTime waitingDeliveryAt;

    /** 双方均发货完成的时间 */
    private LocalDateTime bothDeliveredAt;

    /** 交易完成时间 */
    private LocalDateTime completedAt;

    /** 交易终止时间 */
    private LocalDateTime terminatedAt;

    /** 乐观锁版本号，MyBatis Plus 自动维护 */
    @Version
    private Integer version;

    /** 逻辑删除：0=正常 1=已删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
