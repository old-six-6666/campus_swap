package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员审核举报 DTO
 * action: 1=处理(内容下架)  2=驳回(内容正常)
 */
@Data
public class PostReportReviewDTO {

    @NotNull(message = "处理操作不能为空")
    @Min(value = 1, message = "无效的处理操作")
    @Max(value = 2, message = "无效的处理操作")
    private Integer action;

    /** 审核备注（action=2 驳回时建议填写） */
    private String remark;
}
