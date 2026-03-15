package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private Long receiverId;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;
}
