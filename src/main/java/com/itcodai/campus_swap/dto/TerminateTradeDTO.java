package com.itcodai.campus_swap.dto;

import lombok.Data;

/**
 * 终止交易请求
 * <p>
 * 交易双方均可发起终止（COMPLETED/TERMINATED 终态除外），管理员也可强制终止。
 */
@Data
public class TerminateTradeDTO {

    /** 终止原因（建议填写，最长 500 字） */
    private String reason;
}
