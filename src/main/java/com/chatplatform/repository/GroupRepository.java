package com.chatplatform.repository;

import com.chatplatform.entity.Group;
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
 * 群组数据访问接口
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, Long>, JpaSpecificationExecutor<Group> {

    /**
     * 根据群号查找群组
     */
    Optional<Group> findByGroupNumber(String groupNumber);

    /**
     * 检查群号是否存在
     */
    boolean existsByGroupNumber(String groupNumber);

    /**
     * 根据群主ID查找群组
     */
    List<Group> findByOwnerId(Long ownerId);

    /**
     * 根据群主ID查找群组（分页）
     */
    Page<Group> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * 根据状态查找群组
     */
    List<Group> findByStatus(Integer status);

    /**
     * 根据状态查找群组（分页）
     */
    Page<Group> findByStatus(Integer status, Pageable pageable);

    /**
     * 根据群名称模糊查询
     */
    List<Group> findByNameContainingIgnoreCase(String name);

    /**
     * 根据群名称模糊查询（分页）
     */
    Page<Group> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 搜索群组（按群名称或群号）
     */
    @Query("SELECT g FROM Group g WHERE " +
           "(g.name LIKE %:keyword% OR g.groupNumber LIKE %:keyword%) " +
           "AND g.status = 1 AND g.joinApproval = 0")
    Page<Group> searchPublicGroups(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 搜索群组（包含所有状态）
     */
    @Query("SELECT g FROM Group g WHERE " +
           "(g.name LIKE %:keyword% OR g.groupNumber LIKE %:keyword%) " +
           "AND g.status = 1")
    Page<Group> searchAllGroups(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 获取用户加入的群组列表
     */
    @Query("SELECT DISTINCT g FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE gm.userId = :userId AND gm.joinStatus = 1 AND g.status = 1")
    List<Group> findUserGroups(@Param("userId") Long userId);

    /**
     * 获取用户加入的群组列表（分页）
     */
    @Query("SELECT DISTINCT g FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE gm.userId = :userId AND gm.joinStatus = 1 AND g.status = 1")
    Page<Group> findUserGroups(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取用户创建的群组列表
     */
    @Query("SELECT g FROM Group g WHERE g.ownerId = :userId AND g.status = 1")
    List<Group> findOwnedGroups(@Param("userId") Long userId);

    /**
     * 获取用户管理的群组列表（包括创建的和管理的）
     */
    @Query("SELECT DISTINCT g FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE (g.ownerId = :userId OR (gm.userId = :userId AND gm.role IN (1, 2))) " +
           "AND gm.joinStatus = 1 AND g.status = 1")
    List<Group> findManagedGroups(@Param("userId") Long userId);

    /**
     * 获取用户管理的群组列表（分页）
     */
    @Query("SELECT DISTINCT g FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE (g.ownerId = :userId OR (gm.userId = :userId AND gm.role IN (1, 2))) " +
           "AND gm.joinStatus = 1 AND g.status = 1")
    Page<Group> findManagedGroups(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取用户待审核的群组申请
     */
    @Query("SELECT DISTINCT g FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE gm.userId = :userId AND gm.joinStatus = 0 AND g.status = 1")
    List<Group> findPendingGroupApplications(@Param("userId") Long userId);

    /**
     * 获取可自由加入的群组列表
     */
    @Query("SELECT g FROM Group g WHERE g.joinApproval = 0 AND g.status = 1")
    List<Group> findPublicGroups();

    /**
     * 获取可自由加入的群组列表（分页）
     */
    @Query("SELECT g FROM Group g WHERE g.joinApproval = 0 AND g.status = 1")
    Page<Group> findPublicGroups(Pageable pageable);

    /**
     * 根据成员数量范围查找群组
     */
    @Query("SELECT g FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE g.status = 1 AND gm.joinStatus = 1 " +
           "GROUP BY g.id HAVING COUNT(gm.userId) BETWEEN :minMembers AND :maxMembers")
    List<Group> findGroupsByMemberCount(@Param("minMembers") Integer minMembers,
                                      @Param("maxMembers") Integer maxMembers);

    /**
     * 获取热门群组（按成员数量排序）
     */
    @Query("SELECT g FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE g.status = 1 AND gm.joinStatus = 1 " +
           "GROUP BY g.id ORDER BY COUNT(gm.userId) DESC")
    List<Group> findPopularGroups(Pageable pageable);

    /**
     * 获取最近创建的群组
     */
    @Query("SELECT g FROM Group g WHERE g.status = 1 ORDER BY g.createdAt DESC")
    List<Group> findRecentlyCreatedGroups(Pageable pageable);

    /**
     * 统计用户的群组数量
     */
    @Query("SELECT COUNT(DISTINCT g) FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE gm.userId = :userId AND gm.joinStatus = 1 AND g.status = 1")
    long countUserGroups(@Param("userId") Long userId);

    /**
     * 统计用户创建的群组数量
     */
    @Query("SELECT COUNT(g) FROM Group g WHERE g.ownerId = :userId AND g.status = 1")
    long countOwnedGroups(@Param("userId") Long userId);

    /**
     * 统计群组数量
     */
    @Query("SELECT COUNT(g) FROM Group g WHERE g.status = 1")
    long countActiveGroups();

    /**
     * 统计各状态群组数量
     */
    @Query("SELECT g.status, COUNT(g) FROM Group g GROUP BY g.status")
    List<Object[]> countGroupsByStatus();

    /**
     * 根据群ID列表查找群组
     */
    @Query("SELECT g FROM Group g WHERE g.id IN :groupIds AND g.status = 1")
    List<Group> findByGroupIds(@Param("groupIds") List<Long> groupIds);

    /**
     * 获取群组统计信息
     */
    // 暂时注释掉原生SQL查询以解决HQL语法问题
    // @Query(value = "SELECT " +
    //        "COUNT(*) as total_groups, " +
    //        "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as active_groups, " +
    //        "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as dissolved_groups, " +
    //        "SUM(CASE WHEN join_approval = 0 THEN 1 ELSE 0 END) as public_groups, " +
    //        "SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) as new_groups_last_30_days " +
    //        "FROM chat_groups", nativeQuery = true)
    // Object[] getGroupStatistics();

    /**
     * 批量更新群组状态
     */
    @Modifying
    @Query("UPDATE Group g SET g.status = :status WHERE g.id IN :groupIds")
    int updateGroupsStatus(@Param("groupIds") List<Long> groupIds, @Param("status") Integer status);

    /**
     * 批量转移群主
     */
    @Modifying
    @Query("UPDATE Group g SET g.ownerId = :newOwnerId WHERE g.id IN :groupIds")
    int transferGroupsOwnership(@Param("groupIds") List<Long> groupIds, @Param("newOwnerId") Long newOwnerId);

    /**
     * 查找即将满员的群组
     */
    @Query("SELECT g FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE g.status = 1 AND gm.joinStatus = 1 " +
           "GROUP BY g.id HAVING COUNT(gm.userId) >= g.maxMembers - 10")
    List<Group> findNearlyFullGroups();

    /**
     * 根据多个条件搜索群组
     */
    @Query("SELECT g FROM Group g WHERE " +
           "(:name IS NULL OR g.name LIKE %:name%) AND " +
           "(:groupNumber IS NULL OR g.groupNumber LIKE %:groupNumber%) AND " +
           "(:joinApproval IS NULL OR g.joinApproval = :joinApproval) AND " +
           "(:status IS NULL OR g.status = :status)")
    Page<Group> searchGroupsWithFilters(@Param("name") String name,
                                      @Param("groupNumber") String groupNumber,
                                      @Param("joinApproval") Integer joinApproval,
                                      @Param("status") Integer status,
                                      Pageable pageable);

    /**
     * 获取用户在群组中的最高权限
     */
    @Query("SELECT MAX(gm.role) FROM GroupMember gm " +
           "WHERE gm.userId = :userId AND gm.groupId = :groupId AND gm.joinStatus = 1")
    Integer getUserMaxRoleInGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);

    /**
     * 统计用户管理的群组数量
     */
    @Query("SELECT COUNT(DISTINCT g) FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.groupId " +
           "WHERE (g.ownerId = :userId OR (gm.userId = :userId AND gm.role IN (1, 2))) " +
           "AND gm.joinStatus = 1 AND g.status = 1")
    long countManagedGroups(@Param("userId") Long userId);
}