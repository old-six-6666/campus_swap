package com.itcodai.campus_swap.dto;

import lombok.Data;

/**
 * 确认发货请求（WAITING_DELIVERY 状态下使用）
 */
@Data
public class DeliverDTO {

    /**
     * 物流凭证信息（选填）
     * <p>
     * 建议格式：{"company":"顺丰速运","trackingNo":"SF1234567890","remark":"已打包寄出"}
     * 前端可传入 JSON 字符串或普通备注文本，最长 500 字符。
     */
    private String logistics;
}
