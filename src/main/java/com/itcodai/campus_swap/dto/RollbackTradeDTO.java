package com.itcodai.campus_swap.dto;

import lombok.Data;

/**
 * 回滚交易状态请求（仅管理员可操作）
 * <p>
 * 允许回滚的范围：
 * <ul>
 *   <li>审核阶段（AUDIT_PENDING / AUDIT_PASSED）→ MATCHED</li>
 *   <li>执行阶段前三步（WAITING_DELIVERY / BOTH_DELIVERED / WAITING_CONFIRM_RECEIPT）
 *       → AUDIT_PASSED（启用审核时）或 MATCHED（无需审核时）</li>
 * </ul>
 * 终态（COMPLETED / TERMINATED / AUDIT_REJECTED）不允许回滚。
 */
@Data
public class RollbackTradeDTO {

    /** 回滚原因（必填，记录到操作日志） */
    private String reason;
}
