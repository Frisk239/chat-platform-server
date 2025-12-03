package com.chatplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * QQ号
     */
    private String qqNumber;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 性别 0:未知 1:男 2:女
     */
    private Integer gender;

    /**
     * 性别描述
     */
    private String genderDescription;

    /**
     * 生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 在线状态 0:离线 1:在线 2:忙碌 3:隐身
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDescription;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 注册时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 是否在线
     */
    private Boolean isOnline;

    /**
     * 创建简化的用户信息（用于列表展示）
     */
    public static UserResponse createSimpleUser(Long id, String qqNumber, String nickname,
                                               String avatarUrl, Integer status) {
        return UserResponse.builder()
                .id(id)
                .qqNumber(qqNumber)
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .status(status)
                .isOnline(status != null && status == 1)
                .build();
    }

    /**
     * 创建完整的用户信息
     */
    public static UserResponse createFullUser(com.chatplatform.entity.User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .qqNumber(user.getQqNumber())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .signature(user.getSignature())
                .gender(user.getGender())
                .genderDescription(user.getGenderDescription())
                .birthday(user.getBirthday())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .statusDescription(user.getStatusDescription())
                .lastLoginTime(user.getLastLoginTime())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isOnline(user.isOnline())
                .build();
    }

    /**
     * 创建登录用户信息（排除敏感信息）
     */
    public static UserResponse createLoginUser(com.chatplatform.entity.User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .qqNumber(user.getQqNumber())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .signature(user.getSignature())
                .gender(user.getGender())
                .genderDescription(user.getGenderDescription())
                .birthday(user.getBirthday())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .statusDescription(user.getStatusDescription())
                .lastLoginTime(user.getLastLoginTime())
                .createdAt(user.getCreatedAt())
                .isOnline(user.isOnline())
                .build();
    }
}