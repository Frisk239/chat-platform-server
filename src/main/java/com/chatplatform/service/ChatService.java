package com.chatplatform.service;

import com.chatplatform.dto.request.MessageSendRequest;
import com.chatplatform.dto.response.MessageResponse;
import com.chatplatform.entity.Message;
import com.chatplatform.entity.MessageAttachment;
import com.chatplatform.entity.GroupMember;
import com.chatplatform.repository.GroupMemberRepository;
import com.chatplatform.repository.MessageAttachmentRepository;
import com.chatplatform.repository.MessageRepository;
import com.chatplatform.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天服务类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final WebSocketSessionManager sessionManager;
    private final UserService userService;

    /**
     * 发送私聊消息
     */
    @Transactional
    public MessageResponse sendPrivateMessage(Long senderId, MessageSendRequest request) {
        log.info("发送私聊消息: 发送者={}, 接收者={}", senderId, request.getReceiverId());

        // 创建消息实体
        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .content(request.getContent())
                .messageType(request.getMessageType())
                .replyToId(request.getReplyToMessageId())
                .status(0) // 已发送
                .createdAt(LocalDateTime.now())
                .build();

        // 保存消息
        message = messageRepository.save(message);

        // 处理附件（如果有）
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            saveMessageAttachments(message.getId(), request.getAttachments());
        }

        // 转换为响应DTO
        MessageResponse response = MessageResponse.fromEntity(message);

        // 设置发送者信息
        setSenderInfo(response);

        log.info("私聊消息发送成功: messageId={}", message.getId());
        return response;
    }

    /**
     * 发送群聊消息
     */
    @Transactional
    public MessageResponse sendGroupMessage(Long senderId, MessageSendRequest request) {
        log.info("发送群聊消息: 发送者={}, 群组={}", senderId, request.getGroupId());

        // 验证用户是否为群成员
        if (!isUserGroupMember(senderId, request.getGroupId())) {
            throw new RuntimeException("用户不是群组成员，无法发送消息");
        }

        // 创建消息实体
        Message message = Message.builder()
                .senderId(senderId)
                .groupId(request.getGroupId())
                .content(request.getContent())
                .messageType(request.getMessageType())
                .replyToId(request.getReplyToMessageId())
                .status(0) // 已发送
                .createdAt(LocalDateTime.now())
                .build();

        // 保存消息
        message = messageRepository.save(message);

        // 处理附件（如果有）
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            saveMessageAttachments(message.getId(), request.getAttachments());
        }

        // 转换为响应DTO
        MessageResponse response = MessageResponse.fromEntity(message);

        // 设置发送者和群组信息
        setSenderInfo(response);
        setGroupInfo(response);

        log.info("群聊消息发送成功: messageId={}", message.getId());
        return response;
    }

    /**
     * 广播消息到群组
     */
    public void broadcastToGroup(Long groupId, Long excludeUserId, Object message) {
        // 获取群成员ID列表（排除发送者）
        List<Long> memberIds = groupMemberRepository
                .findByGroupIdAndJoinStatus(groupId, 1)
                .stream()
                .map(GroupMember::getUserId)
                .filter(userId -> !userId.equals(excludeUserId))
                .collect(Collectors.toList());

        // 批量发送给群成员
        sessionManager.sendToUsers(memberIds, message);

        log.debug("消息已广播到群组 {}: 成员数量={}", groupId, memberIds.size());
    }

    /**
     * 标记消息为已读
     */
    @Transactional
    public void markMessageAsRead(Long userId, Long messageId) {
        log.debug("标记消息为已读: 用户={}, 消息={}", userId, messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));

        // 只有消息接收者才能标记为已读
        if (!userId.equals(message.getReceiverId()) && !isUserGroupMember(userId, message.getGroupId())) {
            throw new RuntimeException("无权限标记此消息为已读");
        }

        // 更新消息状态
        if (message.getStatus() < 2) {
            message.setStatus(2); // 已读
            messageRepository.save(message);
        }

        // 这里可以添加已读状态的详细记录逻辑
        // 例如：创建MessageReadStatus记录
    }

    /**
     * 获取私聊消息历史
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getPrivateMessageHistory(Long userId1, Long userId2, int page, int size) {
        // 获取两个用户之间的私聊消息
        org.springframework.data.domain.Page<Message> messagePage = messageRepository.findPrivateMessages(userId1, userId2,
                org.springframework.data.domain.PageRequest.of(page, size));
        List<Message> messages = messagePage.getContent();

        return messages.stream()
                .map(message -> {
                    MessageResponse response = MessageResponse.fromEntity(message);
                    setSenderInfo(response);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取群聊消息历史
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getGroupMessageHistory(Long groupId, int page, int size) {
        // 获取群聊消息
        org.springframework.data.domain.Page<Message> messagePage = messageRepository.findGroupMessages(groupId,
                org.springframework.data.domain.PageRequest.of(page, size));
        List<Message> messages = messagePage.getContent();

        return messages.stream()
                .map(message -> {
                    MessageResponse response = MessageResponse.fromEntity(message);
                    setSenderInfo(response);
                    setGroupInfo(response);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 撤回消息
     */
    @Transactional
    public void revokeMessage(Long userId, Long messageId) {
        log.info("撤回消息: 用户={}, 消息={}", userId, messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));

        // 只有发送者可以撤回消息
        if (!userId.equals(message.getSenderId())) {
            throw new RuntimeException("只有消息发送者可以撤回消息");
        }

        // 检查是否可以撤回（例如：2分钟内的消息才能撤回）
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(2);
        if (message.getCreatedAt().isBefore(timeLimit)) {
            throw new RuntimeException("消息发送超过2分钟，无法撤回");
        }

        // 撤回消息
        message.setIsRevoked(true);
        message.setRevokeTime(LocalDateTime.now());
        messageRepository.save(message);

        // 通知相关用户消息已撤回
        notifyMessageRevoked(message);
    }

    /**
     * 保存消息附件
     */
    private void saveMessageAttachments(Long messageId, List<MessageSendRequest.MessageAttachmentRequest> attachments) {
        for (MessageSendRequest.MessageAttachmentRequest attachmentRequest : attachments) {
            MessageAttachment attachment = MessageAttachment.builder()
                    .messageId(messageId)
                    .fileName(attachmentRequest.getFileName())
                    .originalName(attachmentRequest.getOriginalName())
                    .fileSize(attachmentRequest.getFileSize())
                    .fileType(attachmentRequest.getFileType())
                    .mimeType(attachmentRequest.getMimeType())
                    .createdAt(LocalDateTime.now())
                    .build();
            messageAttachmentRepository.save(attachment);
        }
    }

    /**
     * 检查用户是否为群成员
     */
    private boolean isUserGroupMember(Long userId, Long groupId) {
        if (groupId == null) {
            return false;
        }
        return groupMemberRepository.existsByGroupIdAndUserIdAndJoinStatus(groupId, userId, 1);
    }

    /**
     * 设置发送者信息
     */
    private void setSenderInfo(MessageResponse response) {
        try {
            var senderInfo = userService.getUserById(response.getSenderId());
            if (senderInfo != null) {
                response.setSenderNickname(senderInfo.getNickname());
                response.setSenderQqNumber(senderInfo.getQqNumber());
                response.setSenderAvatarUrl(senderInfo.getAvatarUrl());
            }
        } catch (Exception e) {
            log.error("获取发送者信息失败: userId={}", response.getSenderId(), e);
        }
    }

    /**
     * 设置群组信息
     */
    private void setGroupInfo(MessageResponse response) {
        // 这里可以添加获取群组信息的逻辑
        // 暂时设置为空
        response.setGroupName("群组名称");
    }

    /**
     * 通知消息已撤回
     */
    private void notifyMessageRevoked(Message message) {
        Map<String, Object> revokeNotification = Map.of(
                "type", "message_revoked",
                "messageId", message.getId(),
                "senderId", message.getSenderId(),
                "revokeTime", message.getRevokeTime(),
                "timestamp", System.currentTimeMillis()
        );

        if (message.isPrivateMessage()) {
            // 私聊消息，通知接收者
            sessionManager.sendToUser(message.getReceiverId(), revokeNotification);
        } else if (message.isGroupMessage()) {
            // 群聊消息，通知群成员（除了发送者）
            broadcastToGroup(message.getGroupId(), message.getSenderId(), revokeNotification);
        }
    }

    /**
     * 获取未读消息数量
     */
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(Long userId) {
        return messageRepository.countUnreadPrivateMessages(userId);
    }

    /**
     * 获取用户的最新消息列表
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getRecentMessages(Long userId, int limit) {
        // 这里可以实现获取用户最新消息的逻辑
        // 包括私聊和群聊消息
        return messageRepository.findAllRelatedMessages(userId,
                org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(message -> {
                    MessageResponse response = MessageResponse.fromEntity(message);
                    setSenderInfo(response);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 发送群系统消息
     */
    @Transactional
    public MessageResponse sendGroupSystemMessage(Long groupId, Long senderId, String content) {
        log.info("发送群系统消息: groupId={}, senderId={}, content={}", groupId, senderId, content);

        // 创建系统消息
        Message message = Message.builder()
                .senderId(senderId)
                .groupId(groupId)
                .content(content)
                .messageType(0) // 文本消息
                .status(0) // 已发送
                .createdAt(LocalDateTime.now())
                .build();

        // 保存消息
        message = messageRepository.save(message);

        // 转换为响应DTO
        MessageResponse response = MessageResponse.fromEntity(message);
        setSenderInfo(response);

        // 广播消息给群组成员
        broadcastToGroup(groupId, senderId, Map.of(
                "type", "group_system_message",
                "message", response,
                "timestamp", System.currentTimeMillis()
        ));

        return response;
    }
}