package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知实体 — 对应表 t_notification
 */
@Data
@TableName("t_notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者（帖主）用户ID */
    private Long receiverId;

    /** 触发通知的用户ID */
    private Long senderId;

    /** 通知类型：LIKE / FAVORITE / COMMENT */
    private String type;

    /** 关联帖子ID */
    private Long postId;

    /** 附加内容（评论时填评论内容的前50字） */
    private String content;

    /** 0-未读  1-已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
