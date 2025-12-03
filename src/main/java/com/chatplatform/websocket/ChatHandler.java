package com.chatplatform.websocket;

import com.alibaba.fastjson2.JSON;
import com.chatplatform.dto.request.MessageSendRequest;
import com.chatplatform.dto.response.ApiResponse;
import com.chatplatform.dto.response.MessageResponse;
import com.chatplatform.entity.Message;
import com.chatplatform.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;

/**
 * 聊天消息WebSocket处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatHandler implements WebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("WebSocket连接建立: {}", session.getId());

        // 从会话属性中获取用户ID
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            String sessionId = UUID.randomUUID().toString();
            sessionManager.addSession(userId, sessionId, session);

            // 发送连接成功消息
            sendConnectionMessage(userId, "连接成功", "CONNECTED");
        } else {
            log.error("无法从会话中获取用户ID，关闭连接");
            session.close();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (!(message instanceof TextMessage)) {
            log.warn("收到非文本消息，忽略: {}", message.getClass().getSimpleName());
            return;
        }

        String payload = ((TextMessage) message).getPayload();
        log.debug("收到WebSocket消息: {}", payload);

        try {
            // 解析消息
            Map<String, Object> messageMap = JSON.parseObject(payload, Map.class);
            String type = (String) messageMap.get("type");

            // 根据消息类型处理
            switch (type) {
                case "ping":
                    handlePing(session);
                    break;
                case "private_message":
                    handlePrivateMessage(session, messageMap);
                    break;
                case "group_message":
                    handleGroupMessage(session, messageMap);
                    break;
                case "message_read":
                    handleMessageRead(session, messageMap);
                    break;
                case "typing":
                    handleTyping(session, messageMap);
                    break;
                case "stop_typing":
                    handleStopTyping(session, messageMap);
                    break;
                default:
                    log.warn("未知的消息类型: {}", type);
                    sendErrorMessage(session, "未知的消息类型: " + type);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", payload, e);
            sendErrorMessage(session, "消息处理失败: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}", session.getId(), exception);

        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionManager.removeSession(userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.debug("WebSocket连接关闭: sessionId={}, status={}", session.getId(), closeStatus);

        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionManager.removeSession(userId);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理心跳消息
     */
    private void handlePing(WebSocketSession session) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId != null) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\",\"timestamp\":" + System.currentTimeMillis() + "}"));
            }
        } catch (Exception e) {
            log.error("发送pong消息失败", e);
        }
    }

    /**
     * 处理私聊消息
     */
    private void handlePrivateMessage(WebSocketSession session, Map<String, Object> messageMap) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                sendErrorMessage(session, "用户未认证");
                return;
            }

            // 转换为发送请求
            MessageSendRequest sendRequest = convertToMessageSendRequest(messageMap);
            MessageSendRequest updatedRequest = MessageSendRequest.builder()
                    .senderId(userId)
                    .receiverId(sendRequest.getReceiverId())
                    .groupId(sendRequest.getGroupId())
                    .content(sendRequest.getContent())
                    .messageType(sendRequest.getMessageType())
                    .replyToMessageId(sendRequest.getReplyToMessageId())
                    .build();

            // 发送消息
            MessageResponse messageResponse = chatService.sendPrivateMessage(userId, updatedRequest);

            // 发送消息给接收者
            if (messageResponse.getReceiverId() != null) {
                Map<String, Object> responseMessage = createMessageResponse(messageResponse, "private_message");
                sessionManager.sendToUser(messageResponse.getReceiverId(), responseMessage);
            }

            // 发送确认消息给发送者
            sendSuccessMessage(session, "消息发送成功", messageResponse);

        } catch (Exception e) {
            log.error("处理私聊消息失败", e);
            sendErrorMessage(session, "发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理群聊消息
     */
    private void handleGroupMessage(WebSocketSession session, Map<String, Object> messageMap) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                sendErrorMessage(session, "用户未认证");
                return;
            }

            // 转换为发送请求
            MessageSendRequest sendRequest = convertToMessageSendRequest(messageMap);
            MessageSendRequest updatedRequest = MessageSendRequest.builder()
                    .senderId(userId)
                    .receiverId(sendRequest.getReceiverId())
                    .groupId(sendRequest.getGroupId())
                    .content(sendRequest.getContent())
                    .messageType(sendRequest.getMessageType())
                    .replyToMessageId(sendRequest.getReplyToMessageId())
                    .build();

            // 发送群消息
            MessageResponse messageResponse = chatService.sendGroupMessage(userId, updatedRequest);

            // 发送消息给群组成员（除了发送者）
            if (messageResponse.getGroupId() != null) {
                Map<String, Object> responseMessage = createMessageResponse(messageResponse, "group_message");
                chatService.broadcastToGroup(messageResponse.getGroupId(), userId, responseMessage);
            }

            // 发送确认消息给发送者
            sendSuccessMessage(session, "群消息发送成功", messageResponse);

        } catch (Exception e) {
            log.error("处理群聊消息失败", e);
            sendErrorMessage(session, "发送群消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理消息已读
     */
    private void handleMessageRead(WebSocketSession session, Map<String, Object> messageMap) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                sendErrorMessage(session, "用户未认证");
                return;
            }

            Long messageId = ((Number) messageMap.get("messageId")).longValue();
            chatService.markMessageAsRead(userId, messageId);

            sendSuccessMessage(session, "消息已标记为已读");

        } catch (Exception e) {
            log.error("处理消息已读失败", e);
            sendErrorMessage(session, "标记消息已读失败: " + e.getMessage());
        }
    }

    /**
     * 处理正在输入
     */
    private void handleTyping(WebSocketSession session, Map<String, Object> messageMap) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                return;
            }

            Long targetUserId = messageMap.get("receiverId") != null ?
                ((Number) messageMap.get("receiverId")).longValue() : null;
            Long groupId = messageMap.get("groupId") != null ?
                ((Number) messageMap.get("groupId")).longValue() : null;

            Map<String, Object> typingMessage = Map.of(
                "type", "typing",
                "userId", userId,
                "timestamp", System.currentTimeMillis()
            );

            if (targetUserId != null) {
                sessionManager.sendToUser(targetUserId, typingMessage);
            } else if (groupId != null) {
                chatService.broadcastToGroup(groupId, userId, typingMessage);
            }

        } catch (Exception e) {
            log.error("处理正在输入消息失败", e);
        }
    }

    /**
     * 处理停止输入
     */
    private void handleStopTyping(WebSocketSession session, Map<String, Object> messageMap) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                return;
            }

            Long targetUserId = messageMap.get("receiverId") != null ?
                ((Number) messageMap.get("receiverId")).longValue() : null;
            Long groupId = messageMap.get("groupId") != null ?
                ((Number) messageMap.get("groupId")).longValue() : null;

            Map<String, Object> stopTypingMessage = Map.of(
                "type", "stop_typing",
                "userId", userId,
                "timestamp", System.currentTimeMillis()
            );

            if (targetUserId != null) {
                sessionManager.sendToUser(targetUserId, stopTypingMessage);
            } else if (groupId != null) {
                chatService.broadcastToGroup(groupId, userId, stopTypingMessage);
            }

        } catch (Exception e) {
            log.error("处理停止输入消息失败", e);
        }
    }

    /**
     * 从会话中获取用户ID
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof String) {
            try {
                return Long.parseLong((String) userId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 转换消息请求
     */
    private MessageSendRequest convertToMessageSendRequest(Map<String, Object> messageMap) {
        return MessageSendRequest.builder()
                .receiverId(messageMap.get("receiverId") != null ? ((Number) messageMap.get("receiverId")).longValue() : null)
                .groupId(messageMap.get("groupId") != null ? ((Number) messageMap.get("groupId")).longValue() : null)
                .content((String) messageMap.get("content"))
                .messageType(messageMap.get("messageType") != null ? ((Number) messageMap.get("messageType")).intValue() : 0)
                .replyToId(messageMap.get("replyToMessageId") != null ? ((Number) messageMap.get("replyToMessageId")).longValue() : null)
                .build();
    }

    /**
     * 创建消息响应
     */
    private Map<String, Object> createMessageResponse(MessageResponse messageResponse, String type) {
        return Map.of(
                "type", type,
                "message", messageResponse,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 发送连接消息
     */
    private void sendConnectionMessage(Long userId, String message, String status) {
        Map<String, Object> response = Map.of(
                "type", "connection",
                "status", status,
                "message", message,
                "onlineUserCount", sessionManager.getOnlineUserCount(),
                "timestamp", System.currentTimeMillis()
        );
        sessionManager.sendToUser(userId, response);
    }

    /**
     * 发送成功消息
     */
    private void sendSuccessMessage(WebSocketSession session, String message) {
        sendSuccessMessage(session, message, null);
    }

    /**
     * 发送成功消息（带数据）
     */
    private void sendSuccessMessage(WebSocketSession session, String message, Object data) {
        Map<String, Object> response = Map.of(
                "type", "success",
                "message", message,
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        sendMessage(session, response);
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String error) {
        Map<String, Object> response = Map.of(
                "type", "error",
                "message", error,
                "timestamp", System.currentTimeMillis()
        );
        sendMessage(session, response);
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败", e);
        }
    }
}