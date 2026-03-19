package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcodai.campus_swap.entity.Notification;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.NotificationMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    @Override
    public void send(Long receiverId, Long senderId, String type, Long postId, String content) {
        // 不给自己发通知
        if (receiverId == null || receiverId.equals(senderId)) {
            return;
        }
        Notification n = new Notification();
        n.setReceiverId(receiverId);
        n.setSenderId(senderId);
        n.setType(type);
        n.setPostId(postId);
        n.setContent(content);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    @Override
    public List<Map<String, Object>> list(Long userId, int page, int size) {
        Page<Notification> p = new Page<>(page, size);
        Page<Notification> result = notificationMapper.selectPage(p,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, userId)
                        .orderByDesc(Notification::getCreatedAt));

        List<Notification> records = result.getRecords();
        if (records.isEmpty()) return Collections.emptyList();

        return records.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("type", n.getType());
            map.put("postId", n.getPostId());
            map.put("content", n.getContent());
            map.put("isRead", n.getIsRead());
            map.put("createdAt", n.getCreatedAt());

            User sender = userMapper.selectById(n.getSenderId());
            if (sender != null) {
                Map<String, Object> senderInfo = new HashMap<>();
                senderInfo.put("id", sender.getId());
                senderInfo.put("nickname", sender.getNickname());
                senderInfo.put("avatar", sender.getAvatar());
                map.put("sender", senderInfo);
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public long countUnread(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, userId)
                        .eq(Notification::getIsRead, 0));
    }

    @Override
    public void markAllRead(Long userId) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getReceiverId, userId)
                        .eq(Notification::getIsRead, 0)
                        .set(Notification::getIsRead, 1));
    }
}
