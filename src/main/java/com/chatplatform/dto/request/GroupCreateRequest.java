package com.chatplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建群组请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateRequest {

    /**
     * 群组名称
     */
    @NotBlank(message = "群组名称不能为空")
    @Size(min = 1, max = 100, message = "群组名称长度必须在1-100个字符之间")
    private String name;

    /**
     * 群组描述
     */
    @Size(max = 500, message = "群组描述不能超过500个字符")
    private String description;

    /**
     * 群头像URL
     */
    @Size(max = 500, message = "群头像URL不能超过500个字符")
    private String avatarUrl;

    /**
     * 最大成员数
     */
    private Integer maxMembers = 500;

    /**
     * 入群审批 0:自由加入 1:需要审批
     */
    private Integer joinApproval = 1;

    /**
     * 初始成员ID列表
     */
    private java.util.List<Long> memberIds;
}