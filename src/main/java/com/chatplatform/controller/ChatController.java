package com.chatplatform.controller;

import com.chatplatform.dto.request.MessageSendRequest;
import com.chatplatform.dto.response.ApiResponse;
import com.chatplatform.dto.response.MessageResponse;
import com.chatplatform.service.ChatService;
import com.chatplatform.util.JwtTokenProvider;
import com.chatplatform.websocket.WebSocketSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天控制器
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "聊天管理", description = "聊天消息相关接口")
public class ChatController {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketSessionManager sessionManager;

    /**
     * 发送私聊消息
     */
    @PostMapping("/private/send")
    @Operation(summary = "发送私聊消息", description = "向指定用户发送私聊消息")
    public ResponseEntity<ApiResponse<MessageResponse>> sendPrivateMessage(
            @Valid @RequestBody MessageSendRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long senderId = getUserIdFromToken(httpRequest);
            // 重新构建请求对象，设置发送者ID
            MessageSendRequest updatedRequest = MessageSendRequest.builder()
                    .senderId(senderId)
                    .receiverId(request.getReceiverId())
                    .groupId(request.getGroupId())
                    .content(request.getContent())
                    .messageType(request.getMessageType())
                    .replyToMessageId(request.getReplyToMessageId())
                    .build();

            MessageResponse messageResponse = chatService.sendPrivateMessage(senderId, updatedRequest);
            return ResponseEntity.ok(ApiResponse.success("消息发送成功", messageResponse));
        } catch (Exception e) {
            log.error("发送私聊消息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 发送群聊消息
     */
    @PostMapping("/group/send")
    @Operation(summary = "发送群聊消息", description = "向指定群组发送消息")
    public ResponseEntity<ApiResponse<MessageResponse>> sendGroupMessage(
            @Valid @RequestBody MessageSendRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long senderId = getUserIdFromToken(httpRequest);
            // 重新构建请求对象，设置发送者ID
            MessageSendRequest updatedRequest = MessageSendRequest.builder()
                    .senderId(senderId)
                    .receiverId(request.getReceiverId())
                    .groupId(request.getGroupId())
                    .content(request.getContent())
                    .messageType(request.getMessageType())
                    .replyToMessageId(request.getReplyToMessageId())
                    .build();

            MessageResponse messageResponse = chatService.sendGroupMessage(senderId, updatedRequest);
            return ResponseEntity.ok(ApiResponse.success("群消息发送成功", messageResponse));
        } catch (Exception e) {
            log.error("发送群聊消息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取私聊消息历史
     */
    @GetMapping("/private/history/{targetUserId}")
    @Operation(summary = "获取私聊消息历史", description = "获取与指定用户的私聊消息历史")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getPrivateMessageHistory(
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable Long targetUserId,
            @Parameter(description = "页码，从0开始", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        try {
            Long currentUserId = getUserIdFromToken(httpRequest);
            List<MessageResponse> messages = chatService.getPrivateMessageHistory(currentUserId, targetUserId, page, size);
            return ResponseEntity.ok(ApiResponse.success(messages));
        } catch (Exception e) {
            log.error("获取私聊消息历史失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取群聊消息历史
     */
    @GetMapping("/group/history/{groupId}")
    @Operation(summary = "获取群聊消息历史", description = "获取指定群组的消息历史")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getGroupMessageHistory(
            @Parameter(description = "群组ID", required = true)
            @PathVariable Long groupId,
            @Parameter(description = "页码，从0开始", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        try {
            List<MessageResponse> messages = chatService.getGroupMessageHistory(groupId, page, size);
            return ResponseEntity.ok(ApiResponse.success(messages));
        } catch (Exception e) {
            log.error("获取群聊消息历史失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/message/{messageId}/read")
    @Operation(summary = "标记消息为已读", description = "标记指定消息为已读状态")
    public ResponseEntity<ApiResponse<String>> markMessageAsRead(
            @Parameter(description = "消息ID", required = true)
            @PathVariable Long messageId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            chatService.markMessageAsRead(userId, messageId);
            return ResponseEntity.ok(ApiResponse.success("消息已标记为已读"));
        } catch (Exception e) {
            log.error("标记消息为已读失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 撤回消息
     */
    @PostMapping("/message/{messageId}/revoke")
    @Operation(summary = "撤回消息", description = "撤回指定消息（仅发送者可操作）")
    public ResponseEntity<ApiResponse<String>> revokeMessage(
            @Parameter(description = "消息ID", required = true)
            @PathVariable Long messageId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            chatService.revokeMessage(userId, messageId);
            return ResponseEntity.ok(ApiResponse.success("消息已撤回"));
        } catch (Exception e) {
            log.error("撤回消息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread/count")
    @Operation(summary = "获取未读消息数量", description = "获取当前用户的未读消息总数")
    public ResponseEntity<ApiResponse<Long>> getUnreadMessageCount(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            long unreadCount = chatService.getUnreadMessageCount(userId);
            return ResponseEntity.ok(ApiResponse.success(unreadCount));
        } catch (Exception e) {
            log.error("获取未读消息数量失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取最新消息列表
     */
    @GetMapping("/messages/recent")
    @Operation(summary = "获取最新消息列表", description = "获取用户的最新聊天消息列表")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getRecentMessages(
            @Parameter(description = "消息数量限制", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<MessageResponse> messages = chatService.getRecentMessages(userId, limit);
            return ResponseEntity.ok(ApiResponse.success(messages));
        } catch (Exception e) {
            log.error("获取最新消息列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取在线用户数量
     */
    @GetMapping("/online/count")
    @Operation(summary = "获取在线用户数量", description = "获取当前在线用户总数")
    public ResponseEntity<ApiResponse<Integer>> getOnlineUserCount() {
        try {
            int onlineCount = sessionManager.getOnlineUserCount();
            return ResponseEntity.ok(ApiResponse.success(onlineCount));
        } catch (Exception e) {
            log.error("获取在线用户数量失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online/users")
    @Operation(summary = "获取在线用户列表", description = "获取当前在线用户ID列表")
    public ResponseEntity<ApiResponse<List<Long>>> getOnlineUsers() {
        try {
            List<Long> onlineUsers = sessionManager.getOnlineUserIds().stream()
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(onlineUsers));
        } catch (Exception e) {
            log.error("获取在线用户列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/online/check/{userId}")
    @Operation(summary = "检查用户是否在线", description = "检查指定用户是否在线")
    public ResponseEntity<ApiResponse<Boolean>> checkUserOnline(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        try {
            boolean isOnline = sessionManager.isUserOnline(userId);
            return ResponseEntity.ok(ApiResponse.success(isOnline));
        } catch (Exception e) {
            log.error("检查用户在线状态失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 从Token中获取用户ID
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}