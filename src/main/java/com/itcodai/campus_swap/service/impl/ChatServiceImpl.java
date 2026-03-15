package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.dto.SendMessageDTO;
import com.itcodai.campus_swap.entity.Conversation;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.entity.Message;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.ConversationMapper;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.mapper.MessageMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.ChatService;
import com.itcodai.campus_swap.vo.ConversationVO;
import com.itcodai.campus_swap.vo.MessageVO;
import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public Long sendMessage(Long senderId, SendMessageDTO dto) {
        Long receiverId = dto.getReceiverId();
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能给自己发消息");
        }

        User receiver = userMapper.selectById(receiverId);
        if (receiver == null || receiver.getStatus() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "接收者不存在或已被禁用");
        }

        long u1 = Math.min(senderId, receiverId);
        long u2 = Math.max(senderId, receiverId);
        long realItemId = (dto.getItemId() != null) ? dto.getItemId() : 0L;

        Conversation conv = findConversation(u1, u2, realItemId);
        if (conv == null) {
            conv = new Conversation();
            conv.setUser1Id(u1);
            conv.setUser2Id(u2);
            conv.setItemId(realItemId);
            conv.setUser1Unread(0);
            conv.setUser2Unread(0);
            try {
                conversationMapper.insert(conv);
            } catch (DuplicateKeyException e) {
                conv = findConversation(u1, u2, realItemId);
            }
        }

        Message msg = new Message();
        msg.setConversationId(conv.getId());
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(dto.getContent());
        msg.setIsRead(0);
        messageMapper.insert(msg);

        // 更新会话：最后消息 + 对方未读 +1
        conv.setLastMsg(dto.getContent());
        conv.setLastMsgTime(LocalDateTime.now());
        if (receiverId == u1) {
            conv.setUser1Unread(conv.getUser1Unread() + 1);
        } else {
            conv.setUser2Unread(conv.getUser2Unread() + 1);
        }
        conversationMapper.updateById(conv);

        return msg.getId();
    }

    @Override
    public PageVO<ConversationVO> listConversations(Long userId, int page, int size) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUser1Id, userId)
                .or()
                .eq(Conversation::getUser2Id, userId);
        // 重新构造以避免 OR 优先级问题
        wrapper = new LambdaQueryWrapper<Conversation>()
                .and(w -> w.eq(Conversation::getUser1Id, userId).or().eq(Conversation::getUser2Id, userId))
                .orderByDesc(Conversation::getLastMsgTime);

        Page<Conversation> pageResult = conversationMapper.selectPage(new Page<>(page, size), wrapper);
        List<Conversation> convs = pageResult.getRecords();

        if (convs.isEmpty()) {
            return PageVO.of(Collections.emptyList(), 0, page, size);
        }

        // 批量查询对方用户信息
        Set<Long> otherUserIds = convs.stream()
                .map(c -> c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id())
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(otherUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询关联商品信息
        Set<Long> itemIds = convs.stream()
                .map(Conversation::getItemId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());
        Map<Long, Item> itemMap = itemIds.isEmpty()
                ? Collections.emptyMap()
                : itemMapper.selectBatchIds(itemIds).stream().collect(Collectors.toMap(Item::getId, i -> i));

        List<ConversationVO> records = convs.stream().map(c -> {
            ConversationVO vo = new ConversationVO();
            vo.setConversationId(c.getId());
            Long otherId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            vo.setOtherUserId(otherId);
            User other = userMap.get(otherId);
            if (other != null) {
                vo.setOtherNickname(other.getNickname());
                vo.setOtherAvatar(other.getAvatar());
            }
            vo.setItemId(c.getItemId());
            if (c.getItemId() != null && c.getItemId() != 0) {
                Item item = itemMap.get(c.getItemId());
                if (item != null) {
                    vo.setItemTitle(item.getTitle());
                    vo.setItemCoverImage(item.getCoverImage());
                }
            }
            vo.setLastMsg(c.getLastMsg());
            vo.setLastMsgTime(c.getLastMsgTime());
            vo.setUnreadCount(c.getUser1Id().equals(userId) ? c.getUser1Unread() : c.getUser2Unread());
            return vo;
        }).collect(Collectors.toList());

        return PageVO.of(records, pageResult.getTotal(), page, size);
    }

    @Override
    public PageVO<MessageVO> listMessages(Long userId, Long conversationId, int page, int size) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该会话");
        }

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getCreatedAt);

        Page<Message> pageResult = messageMapper.selectPage(new Page<>(page, size), wrapper);
        List<Message> messages = pageResult.getRecords();

        if (messages.isEmpty()) {
            return PageVO.of(Collections.emptyList(), 0, page, size);
        }

        Set<Long> senderIds = messages.stream().map(Message::getSenderId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<MessageVO> records = messages.stream().map(m -> {
            MessageVO vo = new MessageVO();
            vo.setId(m.getId());
            vo.setConversationId(m.getConversationId());
            vo.setSenderId(m.getSenderId());
            vo.setReceiverId(m.getReceiverId());
            vo.setContent(m.getContent());
            vo.setIsRead(m.getIsRead());
            vo.setCreatedAt(m.getCreatedAt());
            User sender = userMap.get(m.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());

        return PageVO.of(records, pageResult.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该会话");
        }

        messageMapper.markReadByConversation(conversationId, userId);

        if (conv.getUser1Id().equals(userId)) {
            conv.setUser1Unread(0);
        } else {
            conv.setUser2Unread(0);
        }
        conversationMapper.updateById(conv);
    }

    @Override
    public UnreadCountVO countUnread(Long userId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .and(w -> w.eq(Conversation::getUser1Id, userId).or().eq(Conversation::getUser2Id, userId));

        List<Conversation> convs = conversationMapper.selectList(wrapper);
        long total = convs.stream().mapToLong(c ->
                c.getUser1Id().equals(userId) ? c.getUser1Unread() : c.getUser2Unread()
        ).sum();

        UnreadCountVO vo = new UnreadCountVO();
        vo.setTotalUnread(total);
        return vo;
    }

    // ---- 私有辅助 ----

    private Conversation findConversation(long u1, long u2, long itemId) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUser1Id, u1)
                .eq(Conversation::getUser2Id, u2)
                .eq(Conversation::getItemId, itemId));
    }
}
