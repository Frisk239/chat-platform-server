package com.chatplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者昵称
     */
    private String senderNickname;

    /**
     * 发送者QQ号
     */
    private String senderQqNumber;

    /**
     * 发送者头像
     */
    private String senderAvatarUrl;

    /**
     * 接收者ID（私聊消息）
     */
    private Long receiverId;

    /**
     * 接收者昵称
     */
    private String receiverNickname;

    /**
     * 群组ID（群聊消息）
     */
    private Long groupId;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型 0:文本 1:图片 2:文件 3:语音 4:视频 5:表情
     */
    private Integer messageType;

    /**
     * 消息类型描述
     */
    private String messageTypeDescription;

    /**
     * 消息状态 0:已发送 1:已送达 2:已读
     */
    private Integer status;

    /**
     * 消息状态描述
     */
    private String statusDescription;

    /**
     * 回复的消息信息
     */
    private MessageResponse replyToMessage;

    /**
     * 是否已撤回
     */
    private Boolean isRevoked;

    /**
     * 撤回时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revokeTime;

    /**
     * 消息附件列表
     */
    private List<MessageAttachmentResponse> attachments;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 是否为私聊消息
     */
    public boolean isPrivateMessage() {
        return receiverId != null;
    }

    /**
     * 是否为群聊消息
     */
    public boolean isGroupMessage() {
        return groupId != null;
    }

    /**
     * 是否为文本消息
     */
    public boolean isTextMessage() {
        return messageType == null || messageType == 0;
    }

    /**
     * 是否为图片消息
     */
    public boolean isImageMessage() {
        return messageType != null && messageType == 1;
    }

    /**
     * 是否为文件消息
     */
    public boolean isFileMessage() {
        return messageType != null && messageType == 2;
    }

    /**
     * 是否已撤回
     */
    public boolean isRevoked() {
        return isRevoked != null && isRevoked;
    }

    /**
     * 消息附件响应DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageAttachmentResponse {
        private Long id;
        private String fileName;
        private String originalName;
        private String fileSize;
        private String fileType;
        private String mimeType;
        private String thumbnailUrl;
    }

    /**
     * 从实体创建响应DTO
     */
    public static MessageResponse fromEntity(com.chatplatform.entity.Message message) {
        if (message == null) {
            return null;
        }

        MessageResponseBuilder builder = MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .groupId(message.getGroupId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .status(message.getStatus())
                .isRevoked(message.getIsRevoked())
                .revokeTime(message.getRevokeTime())
                .createdAt(message.getCreatedAt());

        // 设置发送者信息
        if (message.getSender() != null) {
            builder.senderNickname(message.getSender().getNickname())
                   .senderQqNumber(message.getSender().getQqNumber())
                   .senderAvatarUrl(message.getSender().getAvatarUrl());
        }

        // 设置接收者信息
        if (message.getReceiver() != null) {
            builder.receiverNickname(message.getReceiver().getNickname());
        }

        // 设置群组信息
        if (message.getGroup() != null) {
            builder.groupName(message.getGroup().getName());
        }

        // 设置回复消息信息
        if (message.getReplyToMessage() != null) {
            builder.replyToMessage(fromEntity(message.getReplyToMessage()));
        }

        // 设置消息描述
        builder.messageTypeDescription(getMessageTypeDescription(message.getMessageType()))
               .statusDescription(getMessageStatusDescription(message.getStatus()));

        return builder.build();
    }

    private static String getMessageTypeDescription(Integer messageType) {
        if (messageType == null) return "文本";
        switch (messageType) {
            case 0: return "文本";
            case 1: return "图片";
            case 2: return "文件";
            case 3: return "语音";
            case 4: return "视频";
            case 5: return "表情";
            default: return "未知";
        }
    }

    private static String getMessageStatusDescription(Integer status) {
        if (status == null) return "已发送";
        switch (status) {
            case 0: return "已发送";
            case 1: return "已送达";
            case 2: return "已读";
            default: return "未知";
        }
    }
}