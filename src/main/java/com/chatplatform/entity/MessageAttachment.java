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

/**
 * 消息附件实体类
 */
@Entity
@Table(name = "message_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 消息ID
     */
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    /**
     * 文件名（存储在服务器上的文件名）
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 原文件名（用户上传时的原始文件名）
     */
    @Column(name = "original_name", length = 255)
    private String originalName;

    /**
     * 文件路径
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 文件类型
     */
    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    /**
     * MIME类型
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * 缩略图URL
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /**
     * 下载次数
     */
    @Column(name = "download_count")
    private Integer downloadCount = 0;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 关联的消息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    @JsonIgnore
    private Message message;

    /**
     * 获取文件扩展名
     */
    public String getFileExtension() {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : "";
    }

    /**
     * 获取原始文件扩展名
     */
    public String getOriginalFileExtension() {
        if (originalName == null) return getFileExtension();
        int lastDotIndex = originalName.lastIndexOf('.');
        return lastDotIndex > 0 ? originalName.substring(lastDotIndex + 1).toLowerCase() : "";
    }

    /**
     * 是否为图片文件
     */
    public boolean isImage() {
        String ext = getFileExtension();
        return ext.matches("jpg|jpeg|png|gif|bmp|webp|svg");
    }

    /**
     * 是否为视频文件
     */
    public boolean isVideo() {
        String ext = getFileExtension();
        return ext.matches("mp4|avi|mov|wmv|flv|mkv|webm|m4v");
    }

    /**
     * 是否为音频文件
     */
    public boolean isAudio() {
        String ext = getFileExtension();
        return ext.matches("mp3|wav|flac|aac|ogg|wma|m4a");
    }

    /**
     * 是否为文档文件
     */
    public boolean isDocument() {
        String ext = getFileExtension();
        return ext.matches("pdf|doc|docx|xls|xlsx|ppt|pptx|txt");
    }

    /**
     * 是否为压缩文件
     */
    public boolean isArchive() {
        String ext = getFileExtension();
        return ext.matches("zip|rar|7z|tar|gz|bz2");
    }

    /**
     * 格式化文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";

        long size = fileSize;
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 增加下载次数
     */
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }

    /**
     * 是否有缩略图
     */
    public boolean hasThumbnail() {
        return thumbnailUrl != null && !thumbnailUrl.trim().isEmpty();
    }

    /**
     * 获取显示文件名（优先使用原始文件名）
     */
    public String getDisplayName() {
        return originalName != null && !originalName.trim().isEmpty() ? originalName : fileName;
    }
}