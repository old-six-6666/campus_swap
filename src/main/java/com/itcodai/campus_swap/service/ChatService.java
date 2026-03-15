package com.itcodai.campus_swap.service;

import com.itcodai.campus_swap.dto.SendMessageDTO;
import com.itcodai.campus_swap.vo.ConversationVO;
import com.itcodai.campus_swap.vo.MessageVO;
import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.UnreadCountVO;

public interface ChatService {

    Long sendMessage(Long senderId, SendMessageDTO dto);

    PageVO<ConversationVO> listConversations(Long userId, int page, int size);

    PageVO<MessageVO> listMessages(Long userId, Long conversationId, int page, int size);

    void markRead(Long userId, Long conversationId);

    UnreadCountVO countUnread(Long userId);
}
