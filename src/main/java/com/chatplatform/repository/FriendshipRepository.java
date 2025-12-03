package com.chatplatform.repository;

import com.chatplatform.entity.Friendship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 好友关系数据访问接口
 */
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long>, JpaSpecificationExecutor<Friendship> {

    /**
     * 根据用户ID和好友ID查找好友关系
     */
    Optional<Friendship> findByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 检查好友关系是否存在
     */
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 根据用户ID和状态查找好友关系
     */
    List<Friendship> findByUserIdAndStatus(Long userId, Integer status);

    /**
     * 根据好友ID和状态查找好友关系
     */
    List<Friendship> findByFriendIdAndStatus(Long friendId, Integer status);

    /**
     * 获取用户的好友列表（已确认的好友）
     */
    @Query("SELECT f FROM Friendship f WHERE f.userId = :userId AND f.status = 1")
    List<Friendship> findFriendsByUserId(@Param("userId") Long userId);

    /**
     * 获取用户的好友列表（分页）
     */
    @Query("SELECT f FROM Friendship f WHERE f.userId = :userId AND f.status = 1")
    Page<Friendship> findFriendsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取用户收到的好友申请（待确认状态）
     */
    @Query("SELECT f FROM Friendship f WHERE f.friendId = :userId AND f.status = 0")
    List<Friendship> findFriendRequestsByUserId(@Param("userId") Long userId);

    /**
     * 获取用户发出但未处理的好友申请
     */
    @Query("SELECT f FROM Friendship f WHERE f.userId = :userId AND f.status = 0")
    List<Friendship> findPendingRequestsByUserId(@Param("userId") Long userId);

    /**
     * 获取用户收到的好友申请（分页）
     */
    @Query("SELECT f FROM Friendship f WHERE f.friendId = :userId AND f.status = 0")
    Page<Friendship> findFriendRequestsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取所有与用户相关的好友关系（包括发出的和收到的）
     */
    @Query("SELECT f FROM Friendship f WHERE (f.userId = :userId OR f.friendId = :userId)")
    List<Friendship> findAllByUserId(@Param("userId") Long userId);

    /**
     * 获取互为好友的用户列表
     */
    @Query("SELECT f1 FROM Friendship f1 " +
           "WHERE f1.userId = :userId AND f1.status = 1 " +
           "AND EXISTS (SELECT f2 FROM Friendship f2 " +
           "WHERE f2.userId = f1.friendId AND f2.friendId = f1.userId AND f2.status = 1)")
    List<Friendship> findMutualFriendsByUserId(@Param("userId") Long userId);

    /**
     * 检查两个用户是否为好友
     */
    @Query("SELECT COUNT(f) > 0 FROM Friendship f " +
           "WHERE ((f.userId = :userId1 AND f.friendId = :userId2) OR " +
           "(f.userId = :userId2 AND f.friendId = :userId1)) AND f.status = 1")
    boolean areFriends(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 统计用户的好友数量
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.userId = :userId AND f.status = 1")
    long countFriendsByUserId(@Param("userId") Long userId);

    /**
     * 统计用户收到的好友申请数量
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.friendId = :userId AND f.status = 0")
    long countFriendRequestsByUserId(@Param("userId") Long userId);

    /**
     * 根据状态统计用户的好友关系数量
     */
    @Query("SELECT f.status, COUNT(f) FROM Friendship f WHERE f.userId = :userId GROUP BY f.status")
    List<Object[]> countFriendshipsByStatus(@Param("userId") Long userId);

    /**
     * 根据备注搜索好友
     */
    @Query("SELECT f FROM Friendship f WHERE f.userId = :userId AND f.status = 1 AND f.remark LIKE %:remark%")
    List<Friendship> findFriendsByRemark(@Param("userId") Long userId, @Param("remark") String remark);

    /**
     * 根据好友昵称搜索好友（需要连接User表）
     */
    @Query("SELECT f FROM Friendship f " +
           "JOIN f.friend u " +
           "WHERE f.userId = :userId AND f.status = 1 AND " +
           "(f.remark LIKE %:keyword% OR u.nickname LIKE %:keyword% OR u.qqNumber LIKE %:keyword%)")
    List<Friendship> searchFriends(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 根据用户ID和状态查找好友关系（分页）
     */
    Page<Friendship> findByUserIdAndStatus(Long userId, Integer status, Pageable pageable);

    /**
     * 获取最近添加的好友
     */
    @Query("SELECT f FROM Friendship f WHERE f.userId = :userId AND f.status = 1 ORDER BY f.updatedAt DESC")
    List<Friendship> findRecentlyAddedFriends(@Param("userId") Long userId, Pageable pageable);

    /**
     * 批量删除好友关系
     */
    @Modifying
    @Query("DELETE FROM Friendship f WHERE (f.userId = :userId1 AND f.friendId = :userId2) OR " +
           "(f.userId = :userId2 AND f.friendId = :userId1)")
    int deleteFriendship(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 批量更新好友关系状态
     */
    @Modifying
    @Query("UPDATE Friendship f SET f.status = :status WHERE f.id IN :friendshipIds")
    int updateFriendshipStatus(@Param("friendshipIds") List<Long> friendshipIds, @Param("status") Integer status);

    /**
     * 批量更新好友备注
     */
    @Modifying
    @Query("UPDATE Friendship f SET f.remark = :remark WHERE f.userId = :userId AND f.friendId = :friendId")
    int updateFriendRemark(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("remark") String remark);

    /**
     * 获取好友关系统计信息
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total_friendships, " +
           "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as pending_requests, " +
           "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as confirmed_friends, " +
           "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as rejected_requests " +
           "FROM friendships WHERE user_id = :userId", nativeQuery = true)
    Object[] getFriendshipStatistics(@Param("userId") Long userId);

    /**
     * 查找可能认识的人（好友的好友）
     * 暂时注释掉以解决启动问题，待后续实现
     */
    // @Query("SELECT DISTINCT f2.friendId FROM Friendship f1 " +
    //        "JOIN Friendship f2 ON f1.friendId = f2.userId " +
    //        "WHERE f1.userId = :userId AND f1.status = 1 AND f2.status = 1 AND " +
    //        "f2.friendId != :userId AND NOT EXISTS (" +
    //        "SELECT 1 FROM Friendship f3 WHERE f3.userId = :userId AND f3.friendId = f2.friendId AND f3.status = 1" +
    //        ")")
    // List<Long> findFriendsOfFriends(@Param("userId") Long userId);

    /**
     * 根据创建时间范围查找好友关系
     */
    @Query("SELECT f FROM Friendship f WHERE f.userId = :userId AND f.createdAt BETWEEN :startTime AND :endTime")
    List<Friendship> findFriendshipsByTimeRange(@Param("userId") Long userId,
                                             @Param("startTime") java.time.LocalDateTime startTime,
                                             @Param("endTime") java.time.LocalDateTime endTime);

    /**
     * 检查是否有重复的好友申请
     */
    @Query("SELECT f FROM Friendship f WHERE f.userId = :userId AND f.friendId = :friendId AND f.status = 0")
    Optional<Friendship> findPendingFriendRequest(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 根据用户ID和朋友ID和状态查找好友关系
     */
    Optional<Friendship> findByUserIdAndFriendIdAndStatus(Long userId, Long friendId, Integer status);

    /**
     * 删除指定用户和好友的关系
     */
    void deleteByUserIdAndFriendId(Long userId, Long friendId);
}