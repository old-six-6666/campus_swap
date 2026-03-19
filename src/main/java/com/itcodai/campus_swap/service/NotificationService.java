package com.itcodai.campus_swap.service;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    /** 发送通知（自己不通知自己） */
    void send(Long receiverId, Long senderId, String type, Long postId, String content);

    /** 获取当前用户的通知列表（最新50条） */
    List<Map<String, Object>> list(Long userId, int page, int size);

    /** 获取未读通知数 */
    long countUnread(Long userId);

    /** 将所有未读通知标记为已读 */
    void markAllRead(Long userId);
}
