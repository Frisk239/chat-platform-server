package com.chatplatform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友操作请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendActionRequest {

    /**
     * 操作类型：accept（接受） 或 reject（拒绝）
     */
    @NotNull(message = "操作类型不能为空")
    private String action;

    /**
     * 好友关系ID
     */
    @NotNull(message = "好友关系ID不能为空")
    private Long friendshipId;

    /**
     * 好友备注
     */
    private String remark;
}