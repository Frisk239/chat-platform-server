package com.chatplatform.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加入群组请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupJoinRequest {

    /**
     * 群组ID
     */
    private Long groupId;

    /**
     * 群组号（用于通过群号加群）
     */
    private String groupNumber;

    /**
     * 申请消息
     */
    @Size(max = 200, message = "申请消息不能超过200个字符")
    private String message;

    /**
     * 群昵称（加入群组后显示的昵称）
     */
    @Size(max = 100, message = "群昵称不能超过100个字符")
    private String nickname;
}