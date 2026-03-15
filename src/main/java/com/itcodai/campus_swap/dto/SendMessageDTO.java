package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageDTO {

    @NotNull(message = "接收者不能为空")
    private Long receiverId;

    /** 关联商品 ID，可为 null */
    private Long itemId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息内容不能超过1000字")
    private String content;
}
