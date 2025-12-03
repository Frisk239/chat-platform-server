package com.chatplatform.repository;

import com.chatplatform.entity.GroupMember;
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
 * 群成员数据访问接口
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long>, JpaSpecificationExecutor<GroupMember> {

    /**
     * 根据群组ID和用户ID查找群成员
     */
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * 检查用户是否为群成员
     */
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * 根据群组ID查找群成员列表
     */
    List<GroupMember> findByGroupId(Long groupId);

    /**
     * 根据群组ID查找群成员列表（分页）
     */
    Page<GroupMember> findByGroupId(Long groupId, Pageable pageable);

    /**
     * 根据用户ID查找用户加入的群组
     */
    List<GroupMember> findByUserId(Long userId);

    /**
     * 根据用户ID查找用户加入的群组（分页）
     */
    Page<GroupMember> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据群组ID和角色查找群成员
     */
    List<GroupMember> findByGroupIdAndRole(Long groupId, Integer role);

    /**
     * 根据群组ID和状态查找群成员
     */
    List<GroupMember> findByGroupIdAndJoinStatus(Long groupId, Integer joinStatus);

    /**
     * 获取群组的正式成员（已加入状态）
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.joinStatus = 1")
    List<GroupMember> findActiveMembersByGroupId(@Param("groupId") Long groupId);

    /**
     * 获取群组的正式成员（分页）
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.joinStatus = 1")
    Page<GroupMember> findActiveMembersByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    /**
     * 获取群组的管理员和群主
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.role IN (1, 2) AND gm.joinStatus = 1")
    List<GroupMember> findAdminsByGroupId(@Param("groupId") Long groupId);

    /**
     * 获取群组的主管理员（群主）
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.role = 2 AND gm.joinStatus = 1")
    List<GroupMember> findOwnersByGroupId(@Param("groupId") Long groupId);

    /**
     * 获取待审核的群成员申请
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.joinStatus = 0")
    List<GroupMember> findPendingApplicationsByGroupId(@Param("groupId") Long groupId);

    /**
     * 获取用户待审核的群组申请
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.userId = :userId AND gm.joinStatus = 0")
    List<GroupMember> findPendingApplicationsByUserId(@Param("userId") Long userId);

    /**
     * 统计群组成员数量
     */
    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.joinStatus = 1")
    long countActiveMembersByGroupId(@Param("groupId") Long groupId);

    /**
     * 统计用户加入的群组数量
     */
    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.userId = :userId AND gm.joinStatus = 1")
    long countUserGroups(@Param("userId") Long userId);

    /**
     * 统计各角色的群成员数量
     */
    @Query("SELECT gm.role, COUNT(gm) FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.joinStatus = 1 GROUP BY gm.role")
    List<Object[]> countMembersByRole(@Param("groupId") Long groupId);

    /**
     * 统计各状态的群成员数量
     */
    @Query("SELECT gm.joinStatus, COUNT(gm) FROM GroupMember gm WHERE gm.groupId = :groupId GROUP BY gm.joinStatus")
    List<Object[]> countMembersByStatus(@Param("groupId") Long groupId);

    /**
     * 获取用户的最高权限角色
     */
    @Query("SELECT MAX(gm.role) FROM GroupMember gm WHERE gm.userId = :userId AND gm.groupId = :groupId AND gm.joinStatus = 1")
    Integer getUserMaxRole(@Param("userId") Long userId, @Param("groupId") Long groupId);

    /**
     * 检查用户是否为群主
     */
    @Query("SELECT COUNT(gm) > 0 FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.userId = :userId AND gm.role = 2 AND gm.joinStatus = 1")
    boolean isGroupOwner(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 检查用户是否为管理员
     */
    @Query("SELECT COUNT(gm) > 0 FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.userId = :userId AND gm.role = 1 AND gm.joinStatus = 1")
    boolean isGroupAdmin(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 检查用户是否为管理员或群主
     */
    @Query("SELECT COUNT(gm) > 0 FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.userId = :userId AND gm.role IN (1, 2) AND gm.joinStatus = 1")
    boolean isGroupAdminOrOwner(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 获取用户加入的群组ID列表
     */
    @Query("SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId AND gm.joinStatus = 1")
    List<Long> findUserGroupIds(@Param("userId") Long userId);

    /**
     * 根据群组ID列表查找群成员
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId IN :groupIds AND gm.joinStatus = 1")
    List<GroupMember> findByGroupIds(@Param("groupIds") List<Long> groupIds);

    /**
     * 获取最近加入的群成员
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.joinStatus = 1 ORDER BY gm.joinedAt DESC")
    List<GroupMember> findRecentlyJoinedMembers(@Param("groupId") Long groupId, Pageable pageable);

    /**
     * 获取群组活跃成员（根据最后活跃时间，如果有相关字段）
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.joinStatus = 1 ORDER BY gm.updatedAt DESC")
    List<GroupMember> findActiveMembersByLastActivity(@Param("groupId") Long groupId, Pageable pageable);

    /**
     * 根据群昵称搜索群成员
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.nickname LIKE %:nickname% AND gm.joinStatus = 1")
    List<GroupMember> findMembersByNickname(@Param("groupId") Long groupId, @Param("nickname") String nickname);

    /**
     * 批量设置群成员角色
     */
    @Modifying
    @Query("UPDATE GroupMember gm SET gm.role = :role WHERE gm.groupId = :groupId AND gm.userId IN :userIds")
    int updateMemberRole(@Param("groupId") Long groupId, @Param("userIds") List<Long> userIds, @Param("role") Integer role);

    /**
     * 批量设置群成员昵称
     */
    @Modifying
    @Query("UPDATE GroupMember gm SET gm.nickname = :nickname WHERE gm.groupId = :groupId AND gm.userId = :userId")
    int updateMemberNickname(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("nickname") String nickname);

    /**
     * 批量确认群成员加入
     */
    @Modifying
    @Query("UPDATE GroupMember gm SET gm.joinStatus = 1, gm.joinedAt = CURRENT_TIMESTAMP WHERE gm.id IN :memberIds")
    int confirmMembers(@Param("memberIds") List<Long> memberIds);

    /**
     * 批量移除群成员
     */
    @Modifying
    @Query("UPDATE GroupMember gm SET gm.joinStatus = 2, gm.leftAt = CURRENT_TIMESTAMP WHERE gm.groupId = :groupId AND gm.userId IN :userIds")
    int removeMembers(@Param("groupId") Long groupId, @Param("userIds") List<Long> userIds);

    /**
     * 转移群主权限
     */
    @Modifying
    @Query("UPDATE GroupMember gm SET gm.role = CASE " +
           "WHEN gm.userId = :newOwnerId THEN 2 " +
           "WHEN gm.userId = :currentOwnerId THEN 0 " +
           "ELSE gm.role END " +
           "WHERE gm.groupId = :groupId AND gm.joinStatus = 1 AND gm.userId IN (:newOwnerId, :currentOwnerId)")
    int transferOwnership(@Param("groupId") Long groupId, @Param("currentOwnerId") Long currentOwnerId, @Param("newOwnerId") Long newOwnerId);

    /**
     * 提升用户为管理员
     */
    @Modifying
    @Query("UPDATE GroupMember gm SET gm.role = 1 WHERE gm.groupId = :groupId AND gm.userId = :userId AND gm.joinStatus = 1")
    int promoteToAdmin(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 降级管理员为普通成员
     */
    @Modifying
    @Query("UPDATE GroupMember gm SET gm.role = 0 WHERE gm.groupId = :groupId AND gm.userId = :userId AND gm.joinStatus = 1 AND gm.role = 1")
    int demoteToMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * 获取群成员统计信息
     */
    // 暂时注释掉原生SQL查询以解决HQL语法问题
    // @Query(value = "SELECT " +
    //        "COUNT(*) as total_members, " +
    //        "SUM(CASE WHEN join_status = 1 THEN 1 ELSE 0 END) as active_members, " +
    //        "SUM(CASE WHEN join_status = 0 THEN 1 ELSE 0 END) as pending_members, " +
    //        "SUM(CASE WHEN join_status = 2 THEN 1 ELSE 0 END) as left_members, " +
    //        "SUM(CASE WHEN role = 2 THEN 1 ELSE 0 END) as owners, " +
    //        "SUM(CASE WHEN role = 1 THEN 1 ELSE 0 END) as admins, " +
    //        "SUM(CASE WHEN role = 0 THEN 1 ELSE 0 END) as regular_members " +
    //        "FROM group_members WHERE group_id = :groupId", nativeQuery = true)
    // Object[] getMemberStatistics(@Param("groupId") Long groupId);

    /**
     * 查找即将到期或需要关注的群成员（例如长期不活跃的成员）
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.joinStatus = 1 " +
           "AND gm.updatedAt < :threshold ORDER BY gm.updatedAt ASC")
    List<GroupMember> findInactiveMembers(@Param("groupId") Long groupId, @Param("threshold") java.time.LocalDateTime threshold);

    // 添加缺少的方法
    boolean existsByGroupIdAndUserIdAndJoinStatus(Long groupId, Long userId, Integer joinStatus);

    Optional<GroupMember> findByGroupIdAndUserIdAndJoinStatus(Long groupId, Long userId, Integer joinStatus);

    List<GroupMember> findByUserIdAndJoinStatus(Long userId, Integer joinStatus);

    void deleteByGroupId(Long groupId);
}