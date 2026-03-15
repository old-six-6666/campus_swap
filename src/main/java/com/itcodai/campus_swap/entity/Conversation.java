package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 较小 userId */
    private Long user1Id;

    /** 较大 userId */
    private Long user2Id;

    /** 关联商品，0 表示无 */
    private Long itemId;

    private String lastMsg;

    private LocalDateTime lastMsgTime;

    private Integer user1Unread;

    private Integer user2Unread;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
