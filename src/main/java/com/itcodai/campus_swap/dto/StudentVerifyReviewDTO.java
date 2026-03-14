package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员审核学生认证申请
 * action: 1-通过  2-拒绝
 */
@Data
public class StudentVerifyReviewDTO {

    @NotNull(message = "审核操作不能为空")
    private Integer action;

    private String remark;
}
