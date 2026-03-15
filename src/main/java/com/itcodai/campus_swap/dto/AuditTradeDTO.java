package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核员处理交易审核请求（仅平台审核模式使用）
 */
@Data
public class AuditTradeDTO {

    /** 审核结果：true=通过  false=驳回 */
    @NotNull(message = "请填写审核结果")
    private Boolean passed;

    /** 审核备注（驳回时必填，说明驳回原因） */
    private String remark;
}
