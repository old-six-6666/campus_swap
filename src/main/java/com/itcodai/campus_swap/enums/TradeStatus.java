package com.itcodai.campus_swap.enums;

/**
 * 交易状态枚举
 * <p>
 * 流转顺序：初始状态 → 匹配阶段 → 审核阶段（可选）→ 执行阶段 → 终态
 * <ul>
 *   <li>PENDING_MATCH  甲方创建交易单，等待乙方确认</li>
 *   <li>MATCHED        乙方已确认，交易单关联双方物品</li>
 *   <li>AUDIT_PENDING  进入审核队列</li>
 *   <li>AUDIT_PASSED   审核通过，可进入执行阶段</li>
 *   <li>AUDIT_REJECTED 审核驳回，交易终止</li>
 *   <li>WAITING_DELIVERY 双方准备发货，等待发货（48h 超时）</li>
 *   <li>BOTH_DELIVERED  双方均已发货，等待收货确认</li>
 *   <li>WAITING_CONFIRM_RECEIPT 一方已确认收货，等待另一方（72h 超时）</li>
 *   <li>COMPLETED      双方均确认收货，交易结束</li>
 *   <li>TERMINATED     交易因取消/超时/争议而终止</li>
 * </ul>
 */
public enum TradeStatus {

    // ===== 初始状态 =====
    PENDING_MATCH("PENDING_MATCH", "等待匹配"),

    // ===== 匹配阶段 =====
    MATCHED("MATCHED", "已匹配"),

    // ===== 审核阶段（可选）=====
    AUDIT_PENDING("AUDIT_PENDING", "审核中"),
    AUDIT_PASSED("AUDIT_PASSED", "审核通过"),
    AUDIT_REJECTED("AUDIT_REJECTED", "审核驳回"),

    // ===== 执行阶段 =====
    WAITING_DELIVERY("WAITING_DELIVERY", "等待发货"),
    BOTH_DELIVERED("BOTH_DELIVERED", "双方已发货"),
    WAITING_CONFIRM_RECEIPT("WAITING_CONFIRM_RECEIPT", "等待确认收货"),

    // ===== 终态 =====
    COMPLETED("COMPLETED", "交易完成"),
    TERMINATED("TERMINATED", "交易终止");

    private final String code;
    private final String description;

    TradeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否为终态（不允许再流转）
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == TERMINATED || this == AUDIT_REJECTED;
    }

    /**
     * 根据 code 字符串查找枚举，找不到则返回 null
     */
    public static TradeStatus fromCode(String code) {
        if (code == null) return null;
        for (TradeStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
