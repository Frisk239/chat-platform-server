package com.chatplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群组响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

    /**
     * 群组ID
     */
    private Long id;

    /**
     * 群号
     */
    private String groupNumber;

    /**
     * 群组名称
     */
    private String name;

    /**
     * 群组描述
     */
    private String description;

    /**
     * 群头像
     */
    private String avatarUrl;

    /**
     * 群主ID
     */
    private Long ownerId;

    /**
     * 群主昵称
     */
    private String ownerNickname;

    /**
     * 群主QQ号
     */
    private String ownerQqNumber;

    /**
     * 群主头像
     */
    private String ownerAvatarUrl;

    /**
     * 最大成员数
     */
    private Integer maxMembers;

    /**
     * 当前成员数
     */
    private Integer memberCount;

    /**
     * 入群审批 0:自由加入 1:需要审批
     */
    private Integer joinApproval;

    /**
     * 入群审批描述
     */
    private String joinApprovalDescription;

    /**
     * 群状态 0:已解散 1:正常
     */
    private Integer status;

    /**
     * 群状态描述
     */
    private String statusDescription;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 当前用户在群中的角色
     */
    private Integer currentUserRole;

    /**
     * 当前用户在群中的角色描述
     */
    private String currentUserRoleDescription;

    /**
     * 当前用户在群中的昵称
     */
    private String currentUserNickname;

    /**
     * 群成员列表（可选，用于详细信息）
     */
    private List<GroupMemberResponse> members;

    /**
     * 是否为群主
     */
    public boolean isOwner() {
        return currentUserRole != null && currentUserRole == 2;
    }

    /**
     * 是否为管理员
     */
    public boolean isAdministrator() {
        return currentUserRole != null && currentUserRole == 1;
    }

    /**
     * 是否为管理员或群主
     */
    public boolean isAdmin() {
        return isOwner() || isAdministrator();
    }

    /**
     * 是否已满员
     */
    public boolean isFull() {
        return memberCount != null && maxMembers != null && memberCount >= maxMembers;
    }

    /**
     * 是否需要审批加入
     */
    public boolean needsApproval() {
        return joinApproval != null && joinApproval == 1;
    }

    /**
     * 群组是否正常
     */
    public boolean isActive() {
        return status != null && status == 1;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return name != null ? name : "未命名群组";
    }

    /**
     * 从实体创建响应DTO
     */
    public static GroupResponse fromEntity(com.chatplatform.entity.Group group,
                                          Long currentUserId,
                                          com.chatplatform.entity.GroupMember currentUserMember) {
        if (group == null) {
            return null;
        }

        GroupResponseBuilder builder = GroupResponse.builder()
                .id(group.getId())
                .groupNumber(group.getGroupNumber())
                .name(group.getName())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .ownerId(group.getOwnerId())
                .maxMembers(group.getMaxMembers())
                .joinApproval(group.getJoinApproval())
                .joinApprovalDescription(getJoinApprovalDescription(group.getJoinApproval()))
                .status(group.getStatus())
                .statusDescription(getStatusDescription(group.getStatus()))
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt());

        // 设置当前用户信息
        if (currentUserMember != null) {
            builder.currentUserRole(currentUserMember.getRole())
                   .currentUserRoleDescription(getRoleDescription(currentUserMember.getRole()))
                   .currentUserNickname(currentUserMember.getNickname());
        }

        return builder.build();
    }

    /**
     * 从实体创建响应DTO（包含成员列表）
     */
    public static GroupResponse fromEntityWithMembers(com.chatplatform.entity.Group group,
                                                   Long currentUserId,
                                                   com.chatplatform.entity.GroupMember currentUserMember,
                                                   List<com.chatplatform.entity.GroupMember> members) {
        GroupResponse response = fromEntity(group, currentUserId, currentUserMember);

        if (response != null) {
            int memberCount = members != null ? members.size() : 0;
            response.setMemberCount(memberCount);

            // 转换成员列表
            List<GroupMemberResponse> memberResponses = members.stream()
                    .map(GroupMemberResponse::fromEntity)
                    .collect(java.util.stream.Collectors.toList());
            response.setMembers(memberResponses);
        }

        return response;
    }

    private static String getJoinApprovalDescription(Integer joinApproval) {
        if (joinApproval == null) return "需要审批";
        switch (joinApproval) {
            case 0: return "自由加入";
            case 1: return "需要审批";
            default: return "需要审批";
        }
    }

    private static String getStatusDescription(Integer status) {
        if (status == null) return "已解散";
        switch (status) {
            case 0: return "已解散";
            case 1: return "正常";
            default: return "已解散";
        }
    }

    private static String getRoleDescription(Integer role) {
        if (role == null) return "普通成员";
        switch (role) {
            case 0: return "普通成员";
            case 1: return "管理员";
            case 2: return "群主";
            default: return "普通成员";
        }
    }

    /**
     * 群成员响应DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GroupMemberResponse {
        private Long id;
        private Long userId;
        private String userQqNumber;
        private String userNickname;
        private String userAvatarUrl;
        private Integer role;
        private String roleDescription;
        private String nickname;
        private String joinStatusDescription;
        private LocalDateTime joinedAt;
        private LocalDateTime leftAt;

        public static GroupMemberResponse fromEntity(com.chatplatform.entity.GroupMember member) {
            if (member == null) {
                return null;
            }

            return GroupMemberResponse.builder()
                    .id(member.getId())
                    .userId(member.getUserId())
                    .nickname(member.getNickname())
                    .role(member.getRole())
                    .roleDescription(getRoleDescription(member.getRole()))
                    .joinStatusDescription(getJoinStatusDescription(member.getJoinStatus()))
                    .joinedAt(member.getJoinedAt())
                    .leftAt(member.getLeftAt())
                    .build();
        }

        private static String getJoinStatusDescription(Integer joinStatus) {
            if (joinStatus == null) return "未知";
            switch (joinStatus) {
                case 0: return "待审核";
                case 1: return "已加入";
                case 2: return "已退出";
                default: return "未知";
            }
        }
    }
}