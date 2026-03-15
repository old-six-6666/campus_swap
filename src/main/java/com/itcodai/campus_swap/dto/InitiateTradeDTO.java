package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起交易请求（甲方创建交易单）
 */
@Data
public class InitiateTradeDTO {

    /** 甲方提供的物品ID（必填，必须属于当前登录用户且状态为在售已审核） */
    @NotNull(message = "请选择您要交换的物品")
    private Long initiatorItemId;

    /**
     * 乙方物品ID（选填）
     * <p>
     * 若指定则表示甲方明确想要某件物品，乙方在确认匹配时需携带该物品；
     * 若不指定则由乙方在确认匹配时自行选择物品。
     */
    private Long receiverItemId;

    /**
     * 指定乙方用户ID（选填）
     * <p>
     * 若指定则只有该用户可以确认匹配；若不指定则任何用户均可确认匹配。
     */
    private Long receiverId;

    /** 交易说明 / 备注（选填，最长 500 字） */
    private String remark;
}
