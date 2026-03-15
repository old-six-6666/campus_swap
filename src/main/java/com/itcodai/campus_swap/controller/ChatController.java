package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.dto.SendMessageDTO;
import com.itcodai.campus_swap.service.ChatService;
import com.itcodai.campus_swap.vo.ConversationVO;
import com.itcodai.campus_swap.vo.MessageVO;
import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.UnreadCountVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 聊天相关接口
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** POST /api/chat/messages — 发送消息（需登录） */
    @PostMapping("/messages")
    public Result<Long> sendMessage(@Valid @RequestBody SendMessageDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.sendMessage(userId, dto));
    }

    /** GET /api/chat/conversations — 获取会话列表（需登录） */
    @GetMapping("/conversations")
    public Result<PageVO<ConversationVO>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.listConversations(userId, page, size));
    }

    /** GET /api/chat/conversations/{convId}/messages — 获取消息历史（需登录） */
    @GetMapping("/conversations/{convId}/messages")
    public Result<PageVO<MessageVO>> listMessages(
            @PathVariable Long convId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.listMessages(userId, convId, page, size));
    }

    /** PUT /api/chat/conversations/{convId}/read — 标记会话已读（需登录） */
    @PutMapping("/conversations/{convId}/read")
    public Result<Void> markRead(@PathVariable Long convId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatService.markRead(userId, convId);
        return Result.success();
    }

    /** GET /api/chat/unread — 获取未读消息总数（需登录） */
    @GetMapping("/unread")
    public Result<UnreadCountVO> countUnread(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.countUnread(userId));
    }
}
