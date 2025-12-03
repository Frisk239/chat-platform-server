package com.chatplatform.repository;

import com.chatplatform.entity.Message;
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
 * 消息数据访问接口
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    /**
     * 根据发送者ID和接收者ID查找私聊消息
     */
    @Query("SELECT m FROM Message m WHERE " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "AND m.isRevoked = false ORDER BY m.createdAt DESC")
    Page<Message> findPrivateMessages(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);

    /**
     * 根据群组ID查找群聊消息
     */
    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId AND m.isRevoked = false ORDER BY m.createdAt DESC")
    Page<Message> findGroupMessages(@Param("groupId") Long groupId, Pageable pageable);

    /**
     * 根据发送者ID查找发送的消息
     */
    @Query("SELECT m FROM Message m WHERE m.senderId = :senderId AND m.isRevoked = false ORDER BY m.createdAt DESC")
    Page<Message> findSentMessages(@Param("senderId") Long senderId, Pageable pageable);

    /**
     * 根据接收者ID查找收到的私聊消息
     */
    @Query("SELECT m FROM Message m WHERE m.receiverId = :receiverId AND m.isRevoked = false ORDER BY m.createdAt DESC")
    Page<Message> findReceivedMessages(@Param("receiverId") Long receiverId, Pageable pageable);

    /**
     * 根据用户ID查找所有相关的消息（发送和接收的私聊消息）
     */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.senderId = :userId OR m.receiverId = :userId) " +
           "AND m.isRevoked = false AND m.groupId IS NULL " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findAllPrivateMessages(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据用户ID查找所有相关的群聊消息
     */
    @Query("SELECT m FROM Message m JOIN GroupMember gm ON m.groupId = gm.groupId " +
           "WHERE gm.userId = :userId AND gm.joinStatus = 1 AND m.isRevoked = false " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findAllGroupMessages(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据用户ID查找所有相关的消息
     */
    @Query("SELECT DISTINCT m FROM Message m " +
           "LEFT JOIN GroupMember gm ON m.groupId = gm.groupId " +
           "WHERE (m.senderId = :userId OR m.receiverId = :userId OR " +
           "(m.groupId IS NOT NULL AND gm.userId = :userId AND gm.joinStatus = 1)) " +
           "AND m.isRevoked = false " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findAllRelatedMessages(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据消息类型查找消息
     */
    @Query("SELECT m FROM Message m WHERE m.messageType = :messageType AND m.isRevoked = false ORDER BY m.createdAt DESC")
    Page<Message> findMessagesByType(@Param("messageType") Integer messageType, Pageable pageable);

    /**
     * 根据状态查找消息
     */
    @Query("SELECT m FROM Message m WHERE m.status = :status AND m.isRevoked = false ORDER BY m.createdAt DESC")
    Page<Message> findMessagesByStatus(@Param("status") Integer status, Pageable pageable);

    /**
     * 根据回复的消息ID查找回复消息
     */
    @Query("SELECT m FROM Message m WHERE m.replyToId = :replyToId AND m.isRevoked = false ORDER BY m.createdAt DESC")
    List<Message> findReplies(@Param("replyToId") Long replyToId);

    /**
     * 搜索消息内容（私聊）
     */
    @Query("SELECT m FROM Message m WHERE " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "AND m.content LIKE %:keyword% AND m.isRevoked = false " +
           "ORDER BY m.createdAt DESC")
    Page<Message> searchPrivateMessages(@Param("userId1") Long userId1, @Param("userId2") Long userId2,
                                      @Param("keyword") String keyword, Pageable pageable);

    /**
     * 搜索消息内容（群聊）
     */
    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId AND m.content LIKE %:keyword% AND m.isRevoked = false ORDER BY m.createdAt DESC")
    Page<Message> searchGroupMessages(@Param("groupId") Long groupId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据时间范围查找消息（私聊）
     */
    @Query("SELECT m FROM Message m WHERE " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "AND m.createdAt BETWEEN :startTime AND :endTime AND m.isRevoked = false " +
           "ORDER BY m.createdAt DESC")
    List<Message> findPrivateMessagesByTimeRange(@Param("userId1") Long userId1, @Param("userId2") Long userId2,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 根据时间范围查找消息（群聊）
     */
    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId AND m.createdAt BETWEEN :startTime AND :endTime AND m.isRevoked = false ORDER BY m.createdAt DESC")
    List<Message> findGroupMessagesByTimeRange(@Param("groupId") Long groupId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 统计私聊消息数量
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "AND m.isRevoked = false")
    long countPrivateMessages(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 统计群聊消息数量
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.groupId = :groupId AND m.isRevoked = false")
    long countGroupMessages(@Param("groupId") Long groupId);

    /**
     * 统计用户发送的消息数量
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.senderId = :userId AND m.isRevoked = false")
    long countSentMessages(@Param("userId") Long userId);

    /**
     * 统计用户收到的私聊消息数量
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :userId AND m.isRevoked = false")
    long countReceivedMessages(@Param("userId") Long userId);

    /**
     * 根据消息类型统计消息数量
     */
    @Query("SELECT m.messageType, COUNT(m) FROM Message m WHERE " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "AND m.isRevoked = false GROUP BY m.messageType")
    List<Object[]> countPrivateMessagesByType(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 获取最近的消息（用于消息预览）
     */
    @Query("SELECT m FROM Message m WHERE " +
           "((m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
           "(m.senderId = :userId2 AND m.receiverId = :userId1)) " +
           "AND m.isRevoked = false ORDER BY m.createdAt DESC")
    List<Message> findRecentPrivateMessages(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);

    /**
     * 获取群组最近消息
     */
    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId AND m.isRevoked = false ORDER BY m.createdAt DESC")
    List<Message> findRecentGroupMessages(@Param("groupId") Long groupId, Pageable pageable);

    /**
     * 获取未读消息数量（私聊）
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :userId AND m.status < 2 AND m.isRevoked = false")
    long countUnreadPrivateMessages(@Param("userId") Long userId);

    /**
     * 获取未读消息数量（群聊，需要排除已读状态）
     * 暂时注释掉以解决启动问题，待后续实现
     */
    // @Query("SELECT COUNT(m) FROM Message m JOIN GroupMember gm ON m.groupId = gm.groupId " +
    //        "WHERE gm.userId = :userId AND gm.joinStatus = 1 AND m.senderId != :userId " +
    //        "AND NOT EXISTS (SELECT 1 FROM MessageReadStatus mrs " +
    //        "WHERE mrs.messageId = m.id AND mrs.userId = :userId) " +
    //        "AND m.isRevoked = false")
    // long countUnreadGroupMessages(@Param("userId") Long userId);

    /**
     * 批量更新消息状态
     */
    @Modifying
    @Query("UPDATE Message m SET m.status = :status WHERE m.id IN :messageIds")
    int updateMessagesStatus(@Param("messageIds") List<Long> messageIds, @Param("status") Integer status);

    /**
     * 批量撤回消息
     */
    @Modifying
    @Query("UPDATE Message m SET m.isRevoked = true, m.revokeTime = CURRENT_TIMESTAMP WHERE m.id IN :messageIds AND m.senderId = :userId")
    int revokeMessages(@Param("messageIds") List<Long> messageIds, @Param("userId") Long userId);

    /**
     * 删除过期的撤回消息
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.isRevoked = true AND m.revokeTime < :expireTime")
    int deleteExpiredRevokedMessages(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 获取消息统计信息
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total_messages, " +
           "SUM(CASE WHEN is_revoked = 0 THEN 1 ELSE 0 END) as active_messages, " +
           "SUM(CASE WHEN is_revoked = 1 THEN 1 ELSE 0 END) as revoked_messages, " +
           "SUM(CASE WHEN message_type = 0 THEN 1 ELSE 0 END) as text_messages, " +
           "SUM(CASE WHEN message_type = 1 THEN 1 ELSE 0 END) as image_messages, " +
           "SUM(CASE WHEN message_type = 2 THEN 1 ELSE 0 END) as file_messages, " +
           "SUM(CASE WHEN receiver_id IS NOT NULL THEN 1 ELSE 0 END) as private_messages, " +
           "SUM(CASE WHEN group_id IS NOT NULL THEN 1 ELSE 0 END) as group_messages " +
           "FROM messages WHERE created_at >= :since", nativeQuery = true)
    Object[] getMessageStatistics(@Param("since") LocalDateTime since);

    /**
     * 获取用户消息统计
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total_messages, " +
           "SUM(CASE WHEN receiver_id IS NOT NULL THEN 1 ELSE 0 END) as private_messages, " +
           "SUM(CASE WHEN group_id IS NOT NULL THEN 1 ELSE 0 END) as group_messages " +
           "FROM messages WHERE sender_id = :userId AND is_revoked = 0", nativeQuery = true)
    Object[] getUserMessageStatistics(@Param("userId") Long userId);

    /**
     * 查找可以撤回的消息（在指定时间范围内）
     */
    @Query("SELECT m FROM Message m WHERE m.senderId = :userId AND m.isRevoked = false " +
           "AND m.createdAt >= :cutoffTime ORDER BY m.createdAt DESC")
    List<Message> findRevocableMessages(@Param("userId") Long userId, @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 根据多个条件搜索消息
     */
    @Query("SELECT m FROM Message m WHERE " +
           "(:senderId IS NULL OR m.senderId = :senderId) AND " +
           "(:receiverId IS NULL OR m.receiverId = :receiverId) AND " +
           "(:groupId IS NULL OR m.groupId = :groupId) AND " +
           "(:messageType IS NULL OR m.messageType = :messageType) AND " +
           "(:content IS NULL OR m.content LIKE %:content%) AND " +
           "m.isRevoked = false " +
           "ORDER BY m.createdAt DESC")
    Page<Message> searchMessagesWithFilters(@Param("senderId") Long senderId,
                                          @Param("receiverId") Long receiverId,
                                          @Param("groupId") Long groupId,
                                          @Param("messageType") Integer messageType,
                                          @Param("content") String content,
                                          Pageable pageable);
}