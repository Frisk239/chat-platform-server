package com.chatplatform.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * QQ号
     */
    @Column(name = "qq_number", unique = true, nullable = false, length = 20)
    private String qqNumber;

    /**
     * 用户名
     */
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /**
     * 昵称
     */
    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    /**
     * 密码（BCrypt加密）
     */
    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

    /**
     * 头像URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 个性签名
     */
    @Column(name = "signature", length = 200)
    private String signature;

    /**
     * 性别 0:未知 1:男 2:女
     */
    @Column(name = "gender")
    private Integer gender = 0;

    /**
     * 生日
     */
    @Column(name = "birthday")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 在线状态 0:离线 1:在线 2:忙碌 3:隐身
     */
    @Column(name = "status")
    private Integer status = 0;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 获取性别描述
     */
    public String getGenderDescription() {
        if (gender == null) return "未知";
        switch (gender) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (status == null) return "离线";
        switch (status) {
            case 1: return "在线";
            case 2: return "忙碌";
            case 3: return "隐身";
            default: return "离线";
        }
    }

    /**
     * 是否在线
     */
    public boolean isOnline() {
        return status != null && status == 1;
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLoginTime() {
        this.lastLoginTime = LocalDateTime.now();
    }
}