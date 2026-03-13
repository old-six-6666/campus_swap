package com.itcodai.campus_swap.dto;

import lombok.Data;

/**
 * 商品审核请求体
 */
@Data
public class ItemAuditDTO {

    /**
     * 审核结果：1-通过  2-拒绝
     */
    private Integer action;

    /** 备注（拒绝时必填） */
    private String remark;
}
