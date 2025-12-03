package com.chatplatform.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 群成员管理请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberManageRequest {

    /**
     * 群组ID
     */
    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    /**
     * 操作类型：invite（邀请）, remove（移除）, set_role（设置角色）, set_nickname（设置昵称）
     */
    @NotEmpty(message = "操作类型不能为空")
    private String action;

    /**
     * 用户ID列表
     */
    @NotEmpty(message = "用户ID列表不能为空")
    private java.util.List<Long> userIds;

    /**
     * 角色（仅在设置角色时使用）0:普通成员 1:管理员 2:群主
     */
    private Integer role;

    /**
     * 昵称（仅在设置昵称时使用）
     */
    private String nickname;

    /**
     * 用户ID（单个用户操作时使用）
     */
    private Long userId;
}