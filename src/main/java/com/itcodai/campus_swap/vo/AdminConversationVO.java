package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端聊天会话视图
 */
@Data
public class AdminConversationVO {

    private Long conversationId;

    private Long user1Id;
    private String user1Nickname;
    private String user1Avatar;

    private Long user2Id;
    private String user2Nickname;
    private String user2Avatar;

    private Long itemId;
    private String itemTitle;

    private String lastMsg;
    private LocalDateTime lastMsgTime;

    /** user1Unread + user2Unread */
    private Integer totalUnread;

    private LocalDateTime createdAt;
}
