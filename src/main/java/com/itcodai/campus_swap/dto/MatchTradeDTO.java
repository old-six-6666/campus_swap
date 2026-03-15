package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 乙方确认匹配请求
 */
@Data
public class MatchTradeDTO {

    /** 乙方提供的物品ID（必填，必须属于当前登录用户且状态为在售已审核） */
    @NotNull(message = "请选择您要拿来交换的物品")
    private Long receiverItemId;
}
