package com.chatplatform.service;

import com.chatplatform.dto.request.GroupCreateRequest;
import com.chatplatform.dto.request.GroupJoinRequest;
import com.chatplatform.dto.request.GroupMemberManageRequest;
import com.chatplatform.dto.request.GroupUpdateRequest;
import com.chatplatform.dto.response.GroupResponse;
import com.chatplatform.entity.Group;
import com.chatplatform.entity.GroupMember;
import com.chatplatform.entity.User;
import com.chatplatform.repository.GroupMemberRepository;
import com.chatplatform.repository.GroupRepository;
import com.chatplatform.repository.UserRepository;
import com.chatplatform.util.QQNumberGenerator;
import com.chatplatform.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 群组服务类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final QQNumberGenerator qqNumberGenerator;
    private final WebSocketSessionManager sessionManager;
    private final ChatService chatService;

    /**
     * 创建群组
     */
    @Transactional
    public GroupResponse createGroup(Long ownerId, GroupCreateRequest request) {
        log.info("创建群组: 群主={}, 群组名称={}", ownerId, request.getName());

        // 验证群主存在
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("群主不存在"));

        // 生成群号
        String groupNumber = qqNumberGenerator.generateQQNumber();

        // 创建群组
        Group group = Group.builder()
                .groupNumber(groupNumber)
                .name(request.getName())
                .description(request.getDescription())
                .avatarUrl(request.getAvatarUrl())
                .ownerId(ownerId)
                .maxMembers(request.getMaxMembers())
                .joinApproval(request.getJoinApproval())
                .status(1) // 正常
                .createdAt(LocalDateTime.now())
                .build();

        group = groupRepository.save(group);

        // 群主自动加入群组
        GroupMember ownerMember = GroupMember.builder()
                .groupId(group.getId())
                .userId(ownerId)
                .role(2) // 群主
                .nickname("群主")
                .joinStatus(1) // 已加入
                .joinedAt(LocalDateTime.now())
                .build();

        groupMemberRepository.save(ownerMember);

        // 添加初始成员
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            for (Long memberId : request.getMemberIds()) {
                if (!ownerId.equals(memberId)) { // 避免重复添加群主
                    addGroupMember(group.getId(), memberId, 0, null);
                }
            }
        }

        // 转换为响应DTO
        GroupResponse response = GroupResponse.fromEntity(group, ownerId, ownerMember);

        // 设置群主信息
        response.setOwnerNickname(owner.getNickname());
        response.setOwnerQqNumber(owner.getQqNumber());
        response.setOwnerAvatarUrl(owner.getAvatarUrl());
        response.setMemberCount(request.getMemberIds() != null ? request.getMemberIds().size() + 1 : 1);

        log.info("群组创建成功: groupId={}, 群号={}", group.getId(), group.getGroupNumber());
        return response;
    }

    /**
     * 加入群组
     */
    @Transactional
    public GroupResponse joinGroup(Long userId, GroupJoinRequest request) {
        log.info("加入群组: 用户={}, 群组={}", userId, request.getGroupId());

        // 验证用户存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证群组存在
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("群组不存在"));

        // 验证群组状态
        if (group.getStatus() != 1) {
            throw new RuntimeException("群组已解散，无法加入");
        }

        // 验证群成员数量
        long currentMemberCount = groupMemberRepository.countActiveMembersByGroupId(request.getGroupId());
        if (currentMemberCount >= group.getMaxMembers()) {
            throw new RuntimeException("群组成员已满");
        }

        // 检查是否已是群成员
        if (groupMemberRepository.existsByGroupIdAndUserIdAndJoinStatus(request.getGroupId(), userId, 1)) {
            throw new RuntimeException("已经是群成员");
        }

        // 检查是否已有待审核申请
        Optional<GroupMember> existingApplication = groupMemberRepository
                .findByGroupIdAndUserIdAndJoinStatus(request.getGroupId(), userId, 0);

        if (existingApplication.isPresent()) {
            throw new RuntimeException("已有待审核的入群申请");
        }

        GroupMember member = GroupMember.builder()
                .groupId(request.getGroupId())
                .userId(userId)
                .role(0) // 普通成员
                .nickname(request.getNickname())
                .joinStatus(group.getJoinApproval() == 0 ? 1 : 0) // 根据设置决定状态
                .joinedAt(group.getJoinApproval() == 0 ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .build();

        member = groupMemberRepository.save(member);

        // 如果需要审批，创建待审核记录
        if (group.getJoinApproval() == 1) {
            // 通知群主有新成员申请
            notifyGroupMemberApplication(group, user, request.getMessage());
        }

        // 如果自由加入，立即加入并通知
        if (group.getJoinApproval() == 0) {
            member.setJoinStatus(1);
            member.setJoinedAt(LocalDateTime.now());
            member = groupMemberRepository.save(member);

            // 通知群成员有新成员加入
            notifyMemberJoined(group, user);

            // 通知新用户成功加入群组
            notifyJoinGroupSuccess(user, group);
        }

        // 转换为响应DTO
        return GroupResponse.fromEntity(group, userId, member);
    }

    /**
     * 管理群成员（邀请、移除、设置角色等）
     */
    @Transactional
    public void manageGroupMembers(Long userId, GroupMemberManageRequest request) {
        log.info("管理群成员: 用户={}, 群组={}, 操作={}", userId, request.getGroupId(), request.getAction());

        // 验证群组存在
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("群组不存在"));

        // 验证用户权限
        GroupMember currentUser = groupMemberRepository
                .findByGroupIdAndUserIdAndJoinStatus(request.getGroupId(), userId, 1)
                .orElseThrow(() -> new RuntimeException("不是群成员，无权限操作"));

        if (!currentUser.isAdmin() && !"remove".equals(request.getAction())) {
            throw new RuntimeException("只有管理员或群主可以进行此操作");
        }

        switch (request.getAction().toLowerCase()) {
            case "invite":
                inviteMembers(group, request.getUserIds());
                break;
            case "remove":
                removeMembers(group, request.getUserIds());
                break;
            case "set_role":
                setMemberRole(group, request.getUserId(), request.getRole());
                break;
            case "set_nickname":
                setMemberNickname(group, request.getUserId(), request.getNickname());
                break;
            default:
                throw new RuntimeException("无效的操作类型: " + request.getAction());
        }
    }

    /**
     * 离开群组
     */
    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        log.info("离开群组: 用户={}, 群组={}", userId, groupId);

        // 验证群组存在
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群组不存在"));

        // 验证用户是群成员
        GroupMember member = groupMemberRepository
                .findByGroupIdAndUserIdAndJoinStatus(groupId, userId, 1)
                .orElseThrow(() -> new RuntimeException("不是群成员"));

        // 群主不能离开群组，只能解散群组
        if (member.isOwner()) {
            throw new RuntimeException("群主不能离开群组，请先转让群主权限或解散群组");
        }

        // 设置为已退出
        member.setJoinStatus(2);
        member.setLeftAt(LocalDateTime.now());
        groupMemberRepository.save(member);

        // 通知群成员有成员离开
        notifyMemberLeft(group, userId);
    }

    /**
     * 获取用户加入的群组列表
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(Long userId) {
        log.debug("获取用户群组列表: 用户={}", userId);

        List<GroupMember> memberships = groupMemberRepository
                .findByUserIdAndJoinStatus(userId, 1);

        return memberships.stream()
                .map(member -> {
                    Optional<Group> group = groupRepository.findById(member.getGroupId());
                    return group.map(g -> GroupResponse.fromEntity(g, userId, member))
                            .orElse(null);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 获取群组详情
     */
    @Transactional(readOnly = true)
    public GroupResponse getGroupDetail(Long groupId, Long currentUserId) {
        log.debug("获取群组详情: 群组={}", groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群组不存在"));

        List<GroupMember> members = groupMemberRepository
                .findByGroupIdAndJoinStatus(groupId, 1);

        Optional<GroupMember> currentUserMember = members.stream()
                .filter(m -> m.getUserId().equals(currentUserId))
                .findFirst();

        return GroupResponse.fromEntityWithMembers(group, currentUserId, currentUserMember.orElse(null), members);
    }

    /**
     * 更新群组信息
     */
    @Transactional
    public GroupResponse updateGroup(Long userId, Long groupId, GroupUpdateRequest request) {
        log.info("更新群组信息: 用户={}, 群组={}", userId, groupId);

        // 验证群组存在
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群组不存在"));

        // 验证权限（只有群主和管理员可以修改）
        GroupMember member = groupMemberRepository
                .findByGroupIdAndUserIdAndJoinStatus(groupId, userId, 1)
                .orElseThrow(() -> new RuntimeException("不是群成员，无权限修改群组信息"));

        if (!member.isAdmin()) {
            throw new RuntimeException("只有群主和管理员可以修改群组信息");
        }

        // 更新群组信息
        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getAvatarUrl() != null) {
            group.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getMaxMembers() != null) {
            group.setMaxMembers(request.getMaxMembers());
        }
        if (request.getJoinApproval() != null) {
            group.setJoinApproval(request.getJoinApproval());
        }

        group = groupRepository.save(group);

        // 通知群成员群组信息已更新
        notifyGroupInfoUpdated(group);

        return GroupResponse.fromEntity(group, userId, member);
    }

    /**
     * 解散群组
     */
    @Transactional
    public void dissolveGroup(Long userId, Long groupId) {
        log.info("解散群组: 用户={}, 群组={}", userId, groupId);

        // 验证群组存在
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群组不存在"));

        // 验证用户是群主
        if (!userId.equals(group.getOwnerId())) {
            throw new RuntimeException("只有群主可以解散群组");
        }

        // 设置群组为已解散
        group.setStatus(0);
        groupRepository.save(group);

        // 移除所有群成员
        groupMemberRepository.deleteByGroupId(groupId);

        // 通知所有成员群组已解散
        notifyGroupDissolved(group);
    }

    /**
     * 转让群主权限
     */
    @Transactional
    public void transferOwnership(Long currentOwnerId, Long groupId, Long newOwnerId) {
        log.info("转让群主权限: 当前群主={}, 群组={}, 新群主={}", currentOwnerId, groupId, newOwnerId);

        // 验证群组存在
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群组不存在"));

        // 验证当前用户是群主
        if (!currentOwnerId.equals(group.getOwnerId())) {
            throw new RuntimeException("只有群主可以转让群主权限");
        }

        // 验证新群主是群成员
        GroupMember newOwnerMember = groupMemberRepository
                .findByGroupIdAndUserIdAndJoinStatus(groupId, newOwnerId, 1)
                .orElseThrow(() -> new RuntimeException("新群主必须是群成员"));

        // 更新群组所有者
        group.setOwnerId(newOwnerId);
        groupRepository.save(group);

        // 更新成员角色
        newOwnerMember.setRole(2); // 设为新群主
        groupMemberRepository.save(newOwnerMember);

        // 将原群主设为管理员
        GroupMember oldOwnerMember = groupMemberRepository
                .findByGroupIdAndUserIdAndJoinStatus(groupId, currentOwnerId, 1)
                .orElseThrow(() -> new RuntimeException("原群主成员信息不存在"));

        oldOwnerMember.setRole(1); // 设为管理员
        groupMemberRepository.save(oldOwnerMember);

        // 通知群主权限已转让
        notifyOwnershipTransferred(group, currentOwnerId, newOwnerId);
    }

    /**
     * 搜索群组
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> searchGroups(String keyword, int page, int size) {
        log.debug("搜索群组: 关键词={}, page={}, size={}", keyword, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Group> groupPage = groupRepository.searchAllGroups(keyword, pageable);

        return groupPage.stream()
                .<GroupResponse>map(group -> GroupResponse.builder()
                        .id(group.getId())
                        .groupNumber(group.getGroupNumber())
                        .name(group.getName())
                        .description(group.getDescription())
                        .avatarUrl(group.getAvatarUrl())
                        .memberCount((int) groupMemberRepository.countActiveMembersByGroupId(group.getId()))
                        .maxMembers(group.getMaxMembers())
                        .joinApproval(group.getJoinApproval())
                        .status(group.getStatus())
                        .createdAt(group.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取群组统计
     */
    @Transactional(readOnly = true)
    public GroupStats getGroupStats(Long userId) {
        long totalGroups = groupRepository.countManagedGroups(userId);
        long ownedGroups = groupRepository.countOwnedGroups(userId);

        return GroupStats.builder()
                .totalGroups(totalGroups)
                .ownedGroups(ownedGroups)
                .joinedGroups(totalGroups - ownedGroups)
                .build();
    }

    /**
     * 添加群成员
     */
    private void addGroupMember(Long groupId, Long userId, Integer role, String nickname) {
        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .role(role)
                .nickname(nickname)
                .joinStatus(1) // 直接加入
                .joinedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        groupMemberRepository.save(member);

        // 通知新成员加入
        User user = userRepository.findById(userId).orElse(null);
        Group group = groupRepository.findById(groupId).orElse(null);
        if (user != null && group != null) {
            notifyMemberJoined(group, user);
        }
    }

    /**
     * 邀请成员加入群组
     */
    private void inviteMembers(Group group, List<Long> userIds) {
        for (Long userId : userIds) {
            try {
                if (!groupMemberRepository.existsByGroupIdAndUserIdAndJoinStatus(group.getId(), userId, 1)) {
                    if (!groupMemberRepository.existsByGroupIdAndUserIdAndJoinStatus(group.getId(), userId, 0)) {
                        GroupMember member = GroupMember.builder()
                                .groupId(group.getId())
                                .userId(userId)
                                .role(0)
                                .joinStatus(0) // 待审核
                                .createdAt(LocalDateTime.now())
                                .build();
                        groupMemberRepository.save(member);

                        // 通知用户有群组邀请
                        User user = userRepository.findById(userId).orElse(null);
                        if (user != null) {
                            notifyGroupInvitation(group, user);
                        }
                    }
                }
            } catch (Exception e) {
                    log.error("邀请成员加入群组失败: groupId={}, userId={}", group.getId(), userId, e);
                }
        }
    }

    /**
     * 移除成员
     */
    private void removeMembers(Group group, List<Long> userIds) {
        for (Long userId : userIds) {
            try {
                if (userId.equals(group.getOwnerId())) {
                    continue; // 不能移除群主
                }

                Optional<GroupMember> member = groupMemberRepository
                        .findByGroupIdAndUserIdAndJoinStatus(group.getId(), userId, 1);

                if (member.isPresent()) {
                    GroupMember m = member.get();
                    m.setJoinStatus(2);
                    m.setLeftAt(LocalDateTime.now());
                    groupMemberRepository.save(m);

                    // 通知被移除的成员
                    notifyMemberRemoved(group, userId);
                }
            } catch (Exception e) {
                log.error("移除成员失败: groupId={}, userId={}", group.getId(), userId, e);
            }
        }
    }

    /**
     * 设置成员角色
     */
    private void setMemberRole(Group group, Long userId, Integer role) {
        Optional<GroupMember> member = groupMemberRepository
                .findByGroupIdAndUserIdAndJoinStatus(group.getId(), userId, 1);

        if (member.isPresent()) {
            GroupMember m = member.get();

            // 不能将群主降级
            if (m.isOwner() && role < 2) {
                throw new RuntimeException("不能将群主角色降级");
            }

            m.setRole(role);
            groupMemberRepository.save(m);

            // 通知角色变更
            notifyMemberRoleChanged(group, userId, role);
        }
    }

    /**
     * 设置成员昵称
     */
    private void setMemberNickname(Group group, Long userId, String nickname) {
        Optional<GroupMember> member = groupMemberRepository
                .findByGroupIdAndUserIdAndJoinStatus(group.getId(), userId, 1);

        if (member.isPresent()) {
            GroupMember m = member.get();
            m.setNickname(nickname);
            groupMemberRepository.save(m);

            // 通知昵称变更
            notifyMemberNicknameChanged(group, userId, nickname);
        }
    }

    /**
     * 通知群成员申请
     */
    private void notifyGroupMemberApplication(Group group, User user, String message) {
        Map<String, Object> notification = Map.of(
                "type", "group_member_application",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "applicantId", user.getId(),
                "applicantNickname", user.getNickname(),
                "applicantQqNumber", user.getQqNumber(),
                "message", message,
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(group.getOwnerId(), notification);
    }

    /**
     * 通知成员加入
     */
    private void notifyMemberJoined(Group group, User user) {
        Map<String, Object> notification = Map.of(
                "type", "member_joined",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "userId", user.getId(),
                "userNickname", user.getNickname(),
                "userQqNumber", user.getQqNumber(),
                "userAvatarUrl", user.getAvatarUrl(),
                "timestamp", System.currentTimeMillis()
        );

        // 通知群主
        sessionManager.sendToUser(group.getOwnerId(), notification);

        // 通知其他群成员
        chatService.broadcastToGroup(group.getId(), user.getId(), notification);
    }

    /**
     * 通知加入群组成功
     */
    private void notifyJoinGroupSuccess(User user, Group group) {
        Map<String, Object> notification = Map.of(
                "type", "join_group_success",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(user.getId(), notification);
    }

    /**
     * 通知成员离开
     */
    private void notifyMemberLeft(Group group, Long userId) {
        Map<String, Object> notification = Map.of(
                "type", "member_left",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "userId", userId,
                "timestamp", System.currentTimeMillis()
        );

        // 通知群主
        sessionManager.sendToUser(group.getOwnerId(), notification);

        // 通知其他群成员
        chatService.broadcastToGroup(group.getId(), userId, notification);
    }

    /**
     * 通知群信息更新
     */
    private void notifyGroupInfoUpdated(Group group) {
        Map<String, Object> notification = Map.of(
                "type", "group_info_updated",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "avatarUrl", group.getAvatarUrl(),
                "timestamp", System.currentTimeMillis()
        );

        // 通知所有群成员
        List<Long> memberIds = groupMemberRepository
                .findByGroupIdAndJoinStatus(group.getId(), 1)
                .stream()
                .map(GroupMember::getUserId)
                .collect(Collectors.toList());

        sessionManager.sendToUsers(memberIds, notification);
    }

    /**
     * 通知群组解散
     */
    private void notifyGroupDissolved(Group group) {
        Map<String, Object> notification = Map.of(
                "type", "group_dissolved",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "timestamp", System.currentTimeMillis()
        );

        // 通知所有群成员
        List<Long> memberIds = groupMemberRepository
                .findByGroupIdAndJoinStatus(group.getId(), 1)
                .stream()
                .map(GroupMember::getUserId)
                .collect(Collectors.toList());

        sessionManager.sendToUsers(memberIds, notification);
    }

    /**
     * 通知群主权限转让
     */
    private void notifyOwnershipTransferred(Group group, Long oldOwnerId, Long newOwnerId) {
        // 通知新群主
        Map<String, Object> newOwnerNotification = Map.of(
                "type", "ownership_transferred",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "oldOwnerId", oldOwnerId,
                "newOwnerId", newOwnerId,
                "timestamp", System.currentTimeMillis()
        );
        sessionManager.sendToUser(newOwnerId, newOwnerNotification);

        // 通知原群主
        Map<String, Object> oldOwnerNotification = Map.of(
                "type", "ownership_transferred",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "oldOwnerId", oldOwnerId,
                "newOwnerId", newOwnerId,
                "timestamp", System.currentTimeMillis()
        );
        sessionManager.sendToUser(oldOwnerId, oldOwnerNotification);
    }

    /**
     * 通知群组邀请
     */
    private void notifyGroupInvitation(Group group, User user) {
        Map<String, Object> notification = Map.of(
                "type", "group_invitation",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "inviterId", group.getOwnerId(),
                "inviterName", getUserNickname(group.getOwnerId()),
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(user.getId(), notification);
    }

    /**
     * 通知成员被移除
     */
    private void notifyMemberRemoved(Group group, Long userId) {
        Map<String, Object> notification = Map.of(
                "type", "member_removed",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(userId, notification);
    }

    /**
     * 通知成员角色变更
     */
    private void notifyMemberRoleChanged(Group group, Long userId, Integer newRole) {
        Map<String, Object> notification = Map.of(
                "type", "member_role_changed",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "userId", userId,
                "newRole", newRole,
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(userId, notification);
    }

    /**
     * 通知成员昵称变更
     */
    private void notifyMemberNicknameChanged(Group group, Long userId, String newNickname) {
        Map<String, Object> notification = Map.of(
                "type", "member_nickname_changed",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "userId", userId,
                "newNickname", newNickname,
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(userId, notification);
    }

    /**
     * 分页获取用户群组列表
     */
    @Transactional(readOnly = true)
    public Page<GroupResponse> getUserGroups(Long userId, int page, int size, Integer status) {
        log.debug("分页获取用户群组列表: userId={}, page={}, size={}, status={}", userId, page, size, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<GroupMember> memberPage = groupMemberRepository.findByUserId(userId, pageable);

        // 过滤状态
        List<GroupMember> filteredMembers = memberPage.getContent().stream()
                .filter(member -> status == null ? member.getJoinStatus().equals(1) : member.getJoinStatus().equals(status))
                .collect(Collectors.toList());

        // 转换为GroupResponse
        List<GroupResponse> groupResponses = filteredMembers.stream()
                .map(member -> {
                    Group group = member.getGroup();
                    if (group == null) {
                        return null;
                    }
                    return GroupResponse.fromEntity(group, userId, member);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(groupResponses, pageable, groupResponses.size());
    }

    /**
     * 获取用户创建的群组
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> getCreatedGroups(Long userId) {
        log.debug("获取用户创建的群组: userId={}", userId);

        List<Group> groups = groupRepository.findOwnedGroups(userId);

        return groups.stream()
                .map(group -> {
                    GroupMember member = groupMemberRepository
                            .findByGroupIdAndUserId(group.getId(), userId)
                            .filter(m -> m.getJoinStatus().equals(1))
                            .orElse(null);
                    return GroupResponse.fromEntity(group, userId, member);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户管理的群组
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> getManagedGroups(Long userId) {
        log.debug("获取用户管理的群组: userId={}", userId);

        List<GroupMember> managedMembers = groupMemberRepository.findByUserId(userId)
                .stream()
                .filter(member -> List.of(1, 2).contains(member.getRole()) && member.getJoinStatus().equals(1))
                .collect(Collectors.toList());

        return managedMembers.stream()
                .map(member -> {
                    Group group = member.getGroup();
                    if (group == null || group.getStatus() != 1) {
                        return null;
                    }
                    return GroupResponse.fromEntity(group, userId, member);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 搜索群组（用户视角）
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> searchGroups(Long userId, String keyword, String searchType) {
        log.debug("搜索群组: userId={}, keyword={}, searchType={}", userId, keyword, searchType);

        List<Group> groups;
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        switch (searchType.toLowerCase()) {
            case "number":
            case "description":
            default: // name or any other search type
                // 使用searchAllGroups方法搜索群组，它可以搜索名称和群号
                groups = groupRepository.searchAllGroups(keyword, Pageable.unpaged()).getContent()
                        .stream()
                        .filter(group -> group.getStatus().equals(1))
                        .collect(Collectors.toList());
                break;
        }

        return groups.stream()
                .map(group -> {
                    GroupMember member = groupMemberRepository
                            .findByGroupIdAndUserId(group.getId(), userId)
                            .filter(m -> m.getJoinStatus().equals(1))
                            .orElse(null);
                    return GroupResponse.fromEntity(group, userId, member);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取推荐群组
     */
    @Transactional(readOnly = true)
    public List<Group> getRecommendedGroups(Long userId, int limit) {
        log.debug("获取推荐群组: userId={}, limit={}", userId, limit);

        // 获取用户已加入的群组ID
        List<Long> joinedGroupIds = groupMemberRepository
                .findByUserId(userId)
                .stream()
                .filter(member -> member.getJoinStatus().equals(1))
                .map(member -> member.getGroupId())
                .collect(Collectors.toList());

        // 获取推荐群组（排除已加入的和已解散的）
        // 由于没有findRecommendedGroups方法，我们使用热门群组作为替代
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return groupRepository.findPublicGroups(pageable)
                .stream()
                .filter(group -> !joinedGroupIds.contains(group.getId()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 设置群管理员
     */
    @Transactional
    public void setGroupAdmin(Long ownerId, Long groupId, Long userId) {
        log.info("设置群管理员: ownerId={}, groupId={}, userId={}", ownerId, groupId, userId);

        Group group = getGroupEntity(groupId);
        validateGroupOwner(group, ownerId);

        GroupMember member = getGroupMember(groupId, userId);
        validateMemberCanPromote(member);

        // 更新成员角色为管理员
        groupMemberRepository.promoteToAdmin(groupId, userId);

        log.info("管理员设置成功: groupId={}, userId={}", groupId, userId);

        // 发送通知
        notifyMemberRoleChanged(group, userId, 1);
    }

    /**
     * 取消群管理员
     */
    @Transactional
    public void unsetGroupAdmin(Long ownerId, Long groupId, Long userId) {
        log.info("取消群管理员: ownerId={}, groupId={}, userId={}", ownerId, groupId, userId);

        Group group = getGroupEntity(groupId);
        validateGroupOwner(group, ownerId);

        GroupMember member = getGroupMember(groupId, userId);
        validateMemberIsAdmin(member);

        // 更新成员角色为普通成员
        groupMemberRepository.demoteToMember(groupId, userId);

        log.info("管理员取消成功: groupId={}, userId={}", groupId, userId);

        // 发送通知
        notifyMemberRoleChanged(group, userId, 0);
    }

    /**
     * 踢出群成员
     */
    @Transactional
    public void kickGroupMember(Long operatorId, Long groupId, Long memberId) {
        log.info("踢出群成员: operatorId={}, groupId={}, memberId={}", operatorId, groupId, memberId);

        Group group = getGroupEntity(groupId);

        GroupMember operator = getGroupMember(groupId, operatorId);
        GroupMember targetMember = getGroupMember(groupId, memberId);

        validateCanKickMember(operator, targetMember, operatorId.equals(memberId));

        // 更新目标成员状态为已退出
        groupMemberRepository.removeMembers(groupId, List.of(memberId));

        log.info("成员踢出成功: groupId={}, memberId={}", groupId, memberId);

        // 发送通知给被踢的成员
        notifyMemberKicked(group, memberId);

        // 群内通知
        sendGroupSystemMessage(group.getId(), operatorId,
                String.format("成员 %s 已被踢出群聊", getUserNickname(memberId)));
    }

    /**
     * 获取用户昵称
     */
    private String getUserNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .orElse("未知用户");
    }

    /**
     * 验证成员可以被提升
     */
    private void validateMemberCanPromote(GroupMember member) {
        if (member.getJoinStatus() != 1) {
            throw new IllegalArgumentException("该成员未加入群组");
        }
        if (member.getRole() == 2) {
            throw new IllegalArgumentException("不能设置群主为管理员");
        }
        if (member.getRole() == 1) {
            throw new IllegalArgumentException("该成员已经是管理员");
        }
    }

    /**
     * 验证成员是管理员
     */
    private void validateMemberIsAdmin(GroupMember member) {
        if (member.getJoinStatus() != 1) {
            throw new IllegalArgumentException("该成员未加入群组");
        }
        if (member.getRole() != 1) {
            throw new IllegalArgumentException("该成员不是管理员");
        }
    }

    /**
     * 验证是否可以踢出成员
     */
    private void validateCanKickMember(GroupMember operator, GroupMember target, boolean isSelf) {
        if (isSelf) {
            throw new IllegalArgumentException("不能踢出自己，请使用退出群组功能");
        }

        if (target.getJoinStatus() != 1) {
            throw new IllegalArgumentException("目标成员未加入群组");
        }

        // 群主可以踢出任何人（除了自己）
        // 管理员可以踢出普通成员（但不能踢出群主和其他管理员）
        if (operator.getRole() == 2) {
            // 群主权限
            return;
        } else if (operator.getRole() == 1) {
            // 管理员权限
            if (target.getRole() >= 1) {
                throw new IllegalArgumentException("管理员不能踢出其他管理员或群主");
            }
        } else {
            throw new IllegalArgumentException("普通成员没有踢人权限");
        }
    }

    /**
     * 通知成员被踢出
     */
    private void notifyMemberKicked(Group group, Long memberId) {
        Map<String, Object> notification = Map.of(
                "type", "member_kicked",
                "groupId", group.getId(),
                "groupName", group.getName(),
                "groupNumber", group.getGroupNumber(),
                "message", "您已被踢出群组",
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(memberId, notification);
    }

    /**
     * 获取群组实体
     */
    private Group getGroupEntity(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
    }

    /**
     * 获取群组成员
     */
    private GroupMember getGroupMember(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不在群组中"));
    }

    /**
     * 验证群主权限
     */
    private void validateGroupOwner(Group group, Long userId) {
        if (!group.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("只有群主可以执行此操作");
        }
    }

    /**
     * 发送群系统消息
     */
    private void sendGroupSystemMessage(Long groupId, Long senderId, String content) {
        try {
            chatService.sendGroupSystemMessage(groupId, senderId, content);
        } catch (Exception e) {
            log.warn("发送群系统消息失败: groupId={}, content={}", groupId, content, e);
        }
    }

    /**
     * 群组统计信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GroupStats {
        private Long totalGroups;
        private Long ownedGroups;
        private Long joinedGroups;
    }
}