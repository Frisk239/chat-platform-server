package com.chatplatform.repository;

import com.chatplatform.entity.User;
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
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据QQ号查找用户
     */
    Optional<User> findByQqNumber(String qqNumber);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 检查QQ号是否存在
     */
    boolean existsByQqNumber(String qqNumber);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据在线状态查找用户
     */
    List<User> findByStatus(Integer status);

    /**
     * 根据在线状态和时间范围查找活跃用户
     */
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.lastLoginTime >= :since")
    List<User> findActiveUsersByStatusAndSince(@Param("status") Integer status, @Param("since") LocalDateTime since);

    /**
     * 搜索用户（按昵称或QQ号）
     */
    @Query("SELECT u FROM User u WHERE (u.nickname LIKE %:keyword% OR u.qqNumber LIKE %:keyword%) AND u.id != :currentUserId")
    Page<User> searchUsers(@Param("keyword") String keyword, @Param("currentUserId") Long currentUserId, Pageable pageable);

    /**
     * 根据昵称模糊查询用户
     */
    List<User> findByNicknameContainingIgnoreCase(String nickname);

    /**
     * 根据QQ号模糊查询用户
     */
    List<User> findByQqNumberContaining(String qqNumber);

    /**
     * 获取在线用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 1")
    long countOnlineUsers();

    /**
     * 获取总用户数量
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countTotalUsers();

    /**
     * 根据性别统计用户数量
     */
    @Query("SELECT u.gender, COUNT(u) FROM User u GROUP BY u.gender")
    List<Object[]> countUsersByGender();

    /**
     * 根据在线状态统计用户数量
     */
    @Query("SELECT u.status, COUNT(u) FROM User u GROUP BY u.status")
    List<Object[]> countUsersByStatus();

    /**
     * 获取最近注册的用户
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    Page<User> findRecentlyRegisteredUsers(Pageable pageable);

    /**
     * 获取最近登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginTime IS NOT NULL ORDER BY u.lastLoginTime DESC")
    Page<User> findRecentlyActiveUsers(Pageable pageable);

    /**
     * 根据创建时间范围查找用户
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startTime AND :endTime")
    List<User> findUsersByCreatedTimeRange(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 根据最后登录时间范围查找用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginTime BETWEEN :startTime AND :endTime")
    List<User> findUsersByLastLoginTimeRange(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 批量更新用户在线状态
     */
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id IN :userIds")
    int updateUsersStatus(@Param("userIds") List<Long> userIds, @Param("status") Integer status);

    /**
     * 批量更新用户最后登录时间
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginTime = :loginTime WHERE u.id = :userId")
    int updateUserLastLoginTime(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);

    /**
     * 获取用户的好友列表中不包含的用户ID列表（用于推荐新朋友）
     */
    @Query("SELECT u.id FROM User u WHERE u.id NOT IN " +
           "(SELECT f.friendId FROM Friendship f WHERE f.userId = :userId AND f.status = 1) " +
           "AND u.id != :userId")
    List<Long> findNonFriendUserIds(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据多个条件搜索用户
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:nickname IS NULL OR u.nickname LIKE %:nickname%) AND " +
           "(:qqNumber IS NULL OR u.qqNumber LIKE %:qqNumber%) AND " +
           "(:gender IS NULL OR u.gender = :gender) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "u.id != :currentUserId")
    Page<User> searchUsersWithFilters(@Param("nickname") String nickname,
                                    @Param("qqNumber") String qqNumber,
                                    @Param("gender") Integer gender,
                                    @Param("status") Integer status,
                                    @Param("currentUserId") Long currentUserId,
                                    Pageable pageable);

    /**
     * 检查用户是否存在
     */
    boolean existsById(Long id);

    /**
     * 根据ID列表查找用户
     */
    @Query("SELECT u FROM User u WHERE u.id IN :userIds")
    List<User> findByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 获取用户统计信息
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total_users, " +
           "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as online_users, " +
           "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as offline_users, " +
           "SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) as new_users_last_30_days " +
           "FROM users", nativeQuery = true)
    Object[] getUserStatistics();
}