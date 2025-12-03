package com.chatplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {

    /**
     * 好友ID
     */
    @NotNull(message = "好友ID不能为空")
    private Long friendId;

    /**
     * 申请消息
     */
    @NotBlank(message = "申请消息不能为空")
    @Size(max = 200, message = "申请消息不能超过200个字符")
    private String message;

    /**
     * 好友备注
     */
    @Size(max = 100, message = "好友备注不能超过100个字符")
    private String remark;
}