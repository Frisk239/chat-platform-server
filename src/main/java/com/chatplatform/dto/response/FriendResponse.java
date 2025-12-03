package com.chatplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {

    /**
     * 好友关系ID
     */
    private Long id;

    /**
     * 好友ID
     */
    private Long friendId;

    /**
     * 好友QQ号
     */
    private String friendQqNumber;

    /**
     * 好友昵称
     */
    private String friendNickname;

    /**
     * 好友头像
     */
    private String friendAvatarUrl;

    /**
     * 好友备注
     */
    private String remark;

    /**
     * 好友在线状态
     */
    private Integer friendStatus;

    /**
     * 好友在线状态描述
     */
    private String friendStatusDescription;

    /**
     * 好友关系状态 0:待确认 1:已确认 2:已拒绝
     */
    private Integer status;

    /**
     * 好友关系状态描述
     */
    private String statusDescription;

    /**
     * 申请消息
     */
    private String requestMessage;

    /**
     * 添加时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 是否为好友
     */
    public boolean isFriend() {
        return status != null && status == 1;
    }

    /**
     * 是否待确认
     */
    public boolean isPending() {
        return status != null && status == 0;
    }

    /**
     * 是否被拒绝
     */
    public boolean isRejected() {
        return status != null && status == 2;
    }

    /**
     * 是否在线
     */
    public boolean isOnline() {
        return friendStatus != null && friendStatus == 1;
    }

    /**
     * 获取显示名称（优先使用备注，其次昵称）
     */
    public String getDisplayName() {
        if (remark != null && !remark.trim().isEmpty()) {
            return remark;
        }
        return friendNickname != null ? friendNickname : "未知用户";
    }

    /**
     * 从实体创建响应DTO
     */
    public static FriendResponse fromEntity(com.chatplatform.entity.Friendship friendship,
                                            com.chatplatform.entity.User friendUser) {
        if (friendship == null) {
            return null;
        }

        FriendResponseBuilder builder = FriendResponse.builder()
                .id(friendship.getId())
                .friendId(friendship.getFriendId())
                .remark(friendship.getRemark())
                .requestMessage(friendship.getRequestMessage())
                .status(friendship.getStatus())
                .statusDescription(getStatusDescription(friendship.getStatus()))
                .createdAt(friendship.getCreatedAt());

        // 设置好友信息
        if (friendUser != null) {
            builder.friendQqNumber(friendUser.getQqNumber())
                   .friendNickname(friendUser.getNickname())
                   .friendAvatarUrl(friendUser.getAvatarUrl())
                   .friendStatus(friendUser.getStatus())
                   .friendStatusDescription(friendUser.getStatusDescription());
        }

        return builder.build();
    }

    private static String getStatusDescription(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待确认";
            case 1: return "已确认";
            case 2: return "已拒绝";
            default: return "未知";
        }
    }
}