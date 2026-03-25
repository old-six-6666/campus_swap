package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppealReviewDTO {

    /** 处理动作：1=已处理  2=已驳回 */
    @NotNull(message = "处理动作不能为空")
    @Min(value = 1, message = "action 取值 1 或 2")
    @Max(value = 2, message = "action 取值 1 或 2")
    private Integer action;

    /** 管理员备注（选填） */
    private String remark;
}
