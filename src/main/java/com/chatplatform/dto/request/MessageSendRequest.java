package com.chatplatform.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 发送消息请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendRequest {

    /**
     * 发送者ID（由后端设置）
     */
    private Long senderId;

    /**
     * 接收者ID（私聊消息）
     */
    private Long receiverId;

    /**
     * 群组ID（群聊消息）
     */
    private Long groupId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容不能超过5000个字符")
    private String content;

    /**
     * 消息类型 0:文本 1:图片 2:文件 3:语音 4:视频 5:表情
     */
    @NotNull(message = "消息类型不能为空")
    private Integer messageType = 0;

    /**
     * 回复的消息ID
     */
    private Long replyToMessageId;

    /**
     * 消息附件列表
     */
    private java.util.List<MessageAttachmentRequest> attachments;

    /**
     * 消息附件请求DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageAttachmentRequest {
        private String fileName;
        private String originalName;
        private String filePath;
        private Long fileSize;
        private String fileType;
        private String mimeType;
    }
}