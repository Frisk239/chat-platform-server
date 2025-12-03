package com.chatplatform.repository;

import com.chatplatform.entity.MessageAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 消息附件数据访问接口
 */
@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long>, JpaSpecificationExecutor<MessageAttachment> {

    /**
     * 根据消息ID查找附件
     */
    List<MessageAttachment> findByMessageId(Long messageId);

    /**
     * 根据消息ID查找附件（分页）
     */
    Page<MessageAttachment> findByMessageId(Long messageId, Pageable pageable);

    /**
     * 根据文件类型查找附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE ma.fileType = :fileType ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findByFileType(@Param("fileType") String fileType);

    /**
     * 根据文件类型查找附件（分页）
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE ma.fileType = :fileType ORDER BY ma.createdAt DESC")
    Page<MessageAttachment> findByFileType(@Param("fileType") String fileType, Pageable pageable);

    /**
     * 根据MIME类型查找附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE ma.mimeType LIKE %:mimeType% ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findByMimeTypeContaining(@Param("mimeType") String mimeType);

    /**
     * 根据用户ID查找附件（通过消息关联）
     */
    @Query("SELECT ma FROM MessageAttachment ma " +
           "JOIN ma.message m " +
           "WHERE m.senderId = :userId ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findBySenderId(@Param("userId") Long userId);

    /**
     * 根据用户ID查找附件（分页）
     */
    @Query("SELECT ma FROM MessageAttachment ma " +
           "JOIN ma.message m " +
           "WHERE m.senderId = :userId ORDER BY ma.createdAt DESC")
    Page<MessageAttachment> findBySenderId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据群组ID查找附件
     */
    @Query("SELECT ma FROM MessageAttachment ma " +
           "JOIN ma.message m " +
           "WHERE m.groupId = :groupId ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findByGroupId(@Param("groupId") Long groupId);

    /**
     * 根据群组ID查找附件（分页）
     */
    @Query("SELECT ma FROM MessageAttachment ma " +
           "JOIN ma.message m " +
           "WHERE m.groupId = :groupId ORDER BY ma.createdAt DESC")
    Page<MessageAttachment> findByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    /**
     * 根据私聊对话查找附件
     */
    @Query("SELECT ma FROM MessageAttachment ma " +
           "JOIN ma.message m " +
           "WHERE ((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findByPrivateChat(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 查找图片附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "(ma.mimeType LIKE 'image/%' OR ma.fileType IN ('jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg')) " +
           "ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findImageAttachments();

    /**
     * 查找图片附件（分页）
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "(ma.mimeType LIKE 'image/%' OR ma.fileType IN ('jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg')) " +
           "ORDER BY ma.createdAt DESC")
    Page<MessageAttachment> findImageAttachments(Pageable pageable);

    /**
     * 查找视频附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "(ma.mimeType LIKE 'video/%' OR ma.fileType IN ('mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm', 'm4v')) " +
           "ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findVideoAttachments();

    /**
     * 查找音频附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "(ma.mimeType LIKE 'audio/%' OR ma.fileType IN ('mp3', 'wav', 'flac', 'aac', 'ogg', 'wma', 'm4a')) " +
           "ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findAudioAttachments();

    /**
     * 查找文档附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "ma.fileType IN ('pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'rtf') " +
           "ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findDocumentAttachments();

    /**
     * 查找压缩文件附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "ma.fileType IN ('zip', 'rar', '7z', 'tar', 'gz', 'bz2') " +
           "ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findArchiveAttachments();

    /**
     * 根据文件大小范围查找附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "ma.fileSize BETWEEN :minSize AND :maxSize ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findByFileSizeRange(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    /**
     * 根据文件大小范围查找附件（分页）
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "ma.fileSize BETWEEN :minSize AND :maxSize ORDER BY ma.createdAt DESC")
    Page<MessageAttachment> findByFileSizeRange(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize, Pageable pageable);

    /**
     * 查找大文件附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE ma.fileSize > :size ORDER BY ma.fileSize DESC")
    List<MessageAttachment> findLargeFiles(@Param("size") Long size);

    /**
     * 根据文件名搜索附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "(ma.fileName LIKE %:keyword% OR ma.originalName LIKE %:keyword%) " +
           "ORDER BY ma.createdAt DESC")
    List<MessageAttachment> searchByFileName(@Param("keyword") String keyword);

    /**
     * 根据文件名搜索附件（分页）
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "(ma.fileName LIKE %:keyword% OR ma.originalName LIKE %:keyword%) " +
           "ORDER BY ma.createdAt DESC")
    Page<MessageAttachment> searchByFileName(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据时间范围查找附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE ma.createdAt BETWEEN :startTime AND :endTime ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据时间范围查找附件（分页）
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE ma.createdAt BETWEEN :startTime AND :endTime ORDER BY ma.createdAt DESC")
    Page<MessageAttachment> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    /**
     * 统计各类型附件数量
     */
    @Query("SELECT ma.fileType, COUNT(ma) FROM MessageAttachment ma GROUP BY ma.fileType")
    List<Object[]> countAttachmentsByFileType();

    /**
     * 统计用户发送的附件数量
     */
    @Query("SELECT COUNT(ma) FROM MessageAttachment ma " +
           "JOIN ma.message m " +
           "WHERE m.senderId = :userId")
    long countAttachmentsBySenderId(@Param("userId") Long userId);

    /**
     * 统计群组中的附件数量
     */
    @Query("SELECT COUNT(ma) FROM MessageAttachment ma " +
           "JOIN ma.message m " +
           "WHERE m.groupId = :groupId")
    long countAttachmentsByGroupId(@Param("groupId") Long groupId);

    /**
     * 统计总附件大小
     */
    @Query("SELECT SUM(ma.fileSize) FROM MessageAttachment ma")
    Long sumAllAttachmentSizes();

    /**
     * 统计用户发送的附件总大小
     */
    @Query("SELECT SUM(ma.fileSize) FROM MessageAttachment ma " +
           "JOIN ma.message m " +
           "WHERE m.senderId = :userId")
    Long sumAttachmentSizesBySenderId(@Param("userId") Long userId);

    /**
     * 获取最近上传的附件
     */
    @Query("SELECT ma FROM MessageAttachment ma ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findRecentlyUploaded(Pageable pageable);

    /**
     * 获取下载次数最多的附件
     */
    @Query("SELECT ma FROM MessageAttachment ma ORDER BY ma.downloadCount DESC")
    List<MessageAttachment> findMostDownloaded(Pageable pageable);

    /**
     * 批量增加下载次数
     */
    @Modifying
    @Query("UPDATE MessageAttachment ma SET ma.downloadCount = ma.downloadCount + 1 WHERE ma.id = :attachmentId")
    int incrementDownloadCount(@Param("attachmentId") Long attachmentId);

    /**
     * 批量更新附件信息
     */
    @Modifying
    @Query("UPDATE MessageAttachment ma SET ma.originalName = :originalName, ma.fileName = :fileName, ma.filePath = :filePath WHERE ma.id = :attachmentId")
    int updateAttachmentInfo(@Param("attachmentId") Long attachmentId,
                           @Param("originalName") String originalName,
                           @Param("fileName") String fileName,
                           @Param("filePath") String filePath);

    /**
     * 删除指定消息的所有附件
     */
    @Modifying
    @Query("DELETE FROM MessageAttachment ma WHERE ma.messageId = :messageId")
    int deleteByMessageId(@Param("messageId") Long messageId);

    /**
     * 删除指定用户的附件
     */
    @Modifying
    @Query("DELETE FROM MessageAttachment ma WHERE ma.id IN " +
           "(SELECT ma.id FROM MessageAttachment ma " +
           "JOIN ma.message m WHERE m.senderId = :userId)")
    int deleteBySenderId(@Param("userId") Long userId);

    /**
     * 获取附件统计信息
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total_attachments, " +
           "SUM(file_size) as total_size, " +
           "SUM(CASE WHEN mime_type LIKE 'image/%' THEN 1 ELSE 0 END) as image_count, " +
           "SUM(CASE WHEN mime_type LIKE 'video/%' THEN 1 ELSE 0 END) as video_count, " +
           "SUM(CASE WHEN mime_type LIKE 'audio/%' THEN 1 ELSE 0 END) as audio_count, " +
           "SUM(CASE WHEN mime_type LIKE 'application/pdf' OR mime_type LIKE 'application/msword' OR mime_type LIKE 'application/vnd%' THEN 1 ELSE 0 END) as document_count, " +
           "SUM(download_count) as total_downloads " +
           "FROM message_attachments WHERE created_at >= :since", nativeQuery = true)
    Object[] getAttachmentStatistics(@Param("since") LocalDateTime since);

    /**
     * 查找没有缩略图的图片附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "(ma.mimeType LIKE 'image/%' OR ma.fileType IN ('jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp')) " +
           "AND (ma.thumbnailUrl IS NULL OR ma.thumbnailUrl = '')")
    List<MessageAttachment> findImagesWithoutThumbnail();

    /**
     * 批量更新缩略图URL
     */
    @Modifying
    @Query("UPDATE MessageAttachment ma SET ma.thumbnailUrl = :thumbnailUrl WHERE ma.id = :attachmentId")
    int updateThumbnailUrl(@Param("attachmentId") Long attachmentId, @Param("thumbnailUrl") String thumbnailUrl);

    /**
     * 根据多个条件搜索附件
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE " +
           "(:senderId IS NULL OR EXISTS (SELECT 1 FROM Message m WHERE m.id = ma.messageId AND m.senderId = :senderId)) AND " +
           "(:groupId IS NULL OR EXISTS (SELECT 1 FROM Message m WHERE m.id = ma.messageId AND m.groupId = :groupId)) AND " +
           "(:fileType IS NULL OR ma.fileType = :fileType) AND " +
           "(:minSize IS NULL OR ma.fileSize >= :minSize) AND " +
           "(:maxSize IS NULL OR ma.fileSize <= :maxSize) AND " +
           "(:keyword IS NULL OR ma.fileName LIKE %:keyword% OR ma.originalName LIKE %:keyword%) " +
           "ORDER BY ma.createdAt DESC")
    Page<MessageAttachment> searchAttachmentsWithFilters(@Param("senderId") Long senderId,
                                                       @Param("groupId") Long groupId,
                                                       @Param("fileType") String fileType,
                                                       @Param("minSize") Long minSize,
                                                       @Param("maxSize") Long maxSize,
                                                       @Param("keyword") String keyword,
                                                       Pageable pageable);
}