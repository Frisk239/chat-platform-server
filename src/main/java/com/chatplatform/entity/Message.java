package com.chatplatform.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息实体类
 */
@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 发送者ID
     */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /**
     * 接收者ID（私聊消息）
     */
    @Column(name = "receiver_id")
    private Long receiverId;

    /**
     * 群组ID（群聊消息）
     */
    @Column(name = "group_id")
    private Long groupId;

    /**
     * 消息内容
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 消息类型 0:文本 1:图片 2:文件 3:语音 4:视频 5:表情
     */
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private Integer messageType = 0;

    /**
     * 消息状态 0:已发送 1:已送达 2:已读
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = 0;

    /**
     * 回复的消息ID
     */
    @Column(name = "reply_to_id")
    private Long replyToId;

    /**
     * 是否撤回 0:否 1:是
     */
    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;

    /**
     * 撤回时间
     */
    @Column(name = "revoke_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revokeTime;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 发送者信息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    @JsonIgnore
    private User sender;

    /**
     * 接收者信息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", insertable = false, updatable = false)
    @JsonIgnore
    private User receiver;

    /**
     * 群组信息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    @JsonIgnore
    private Group group;

    /**
     * 回复的消息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id", insertable = false, updatable = false)
    @JsonIgnore
    private Message replyToMessage;

    /**
     * 消息附件列表
     */
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<MessageAttachment> attachments;

    /**
     * 获取消息类型描述
     */
    public String getMessageTypeDescription() {
        if (messageType == null) return "文本";
        switch (messageType) {
            case 0: return "文本";
            case 1: return "图片";
            case 2: return "文件";
            case 3: return "语音";
            case 4: return "视频";
            case 5: return "表情";
            default: return "文本";
        }
    }

    /**
     * 获取消息状态描述
     */
    public String getStatusDescription() {
        if (status == null) return "已发送";
        switch (status) {
            case 0: return "已发送";
            case 1: return "已送达";
            case 2: return "已读";
            default: return "已发送";
        }
    }

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
     * 是否为语音消息
     */
    public boolean isVoiceMessage() {
        return messageType != null && messageType == 3;
    }

    /**
     * 是否为视频消息
     */
    public boolean isVideoMessage() {
        return messageType != null && messageType == 4;
    }

    /**
     * 是否为表情消息
     */
    public boolean isEmojiMessage() {
        return messageType != null && messageType == 5;
    }

    /**
     * 是否已撤回
     */
    public boolean isActuallyRevoked() {
        return isRevoked != null && isRevoked;
    }

    /**
     * 是否已送达
     */
    public boolean isDelivered() {
        return status != null && status >= 1;
    }

    /**
     * 是否已读
     */
    public boolean isRead() {
        return status != null && status >= 2;
    }

    /**
     * 标记为已送达
     */
    public void markAsDelivered() {
        this.status = Math.max(this.status, 1);
    }

    /**
     * 标记为已读
     */
    public void markAsRead() {
        this.status = Math.max(this.status, 2);
    }

    /**
     * 撤回消息
     */
    public void revoke() {
        this.isRevoked = true;
        this.revokeTime = LocalDateTime.now();
    }

    /**
     * 获取接收方ID（私聊消息返回接收者ID，群聊消息返回群组ID）
     */
    public Long getReceiverIdForRouting() {
        return isPrivateMessage() ? receiverId : groupId;
    }
}