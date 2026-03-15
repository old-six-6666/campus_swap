package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {

    private Long conversationId;
    private Long otherUserId;
    private String otherNickname;
    private String otherAvatar;
    private Long itemId;
    private String itemTitle;
    private String itemCoverImage;
    private String lastMsg;
    private LocalDateTime lastMsgTime;
    private Integer unreadCount;
}
