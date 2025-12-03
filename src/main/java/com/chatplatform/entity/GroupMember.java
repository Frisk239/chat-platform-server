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

import java.time.LocalDateTime;

/**
 * 群成员实体类
 */
@Entity
@Table(name = "group_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 群组ID
     */
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 角色 0:普通成员 1:管理员 2:群主
     */
    @Column(name = "role", nullable = false)
    private Integer role = 0;

    /**
     * 群昵称
     */
    @Column(name = "nickname", length = 100)
    private String nickname;

    /**
     * 加入状态 0:待审核 1:已加入 2:已退出
     */
    @Column(name = "join_status", nullable = false)
    private Integer joinStatus = 1;

    /**
     * 加入时间
     */
    @Column(name = "joined_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinedAt;

    /**
     * 离开时间
     */
    @Column(name = "left_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime leftAt;

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
     * 群组信息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    @JsonIgnore
    private Group group;

    /**
     * 用户信息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    /**
     * 获取角色描述
     */
    public String getRoleDescription() {
        if (role == null) return "普通成员";
        switch (role) {
            case 0: return "普通成员";
            case 1: return "管理员";
            case 2: return "群主";
            default: return "普通成员";
        }
    }

    /**
     * 获取加入状态描述
     */
    public String getJoinStatusDescription() {
        if (joinStatus == null) return "未知";
        switch (joinStatus) {
            case 0: return "待审核";
            case 1: return "已加入";
            case 2: return "已退出";
            default: return "未知";
        }
    }

    /**
     * 是否为群主
     */
    public boolean isOwner() {
        return role != null && role == 2;
    }

    /**
     * 是否为管理员
     */
    public boolean isAdministrator() {
        return role != null && role == 1;
    }

    /**
     * 是否为管理员或群主
     */
    public boolean isAdmin() {
        return role != null && (role == 1 || role == 2);
    }

    /**
     * 是否已加入群组
     */
    public boolean hasJoined() {
        return joinStatus != null && joinStatus == 1;
    }

    /**
     * 是否待审核
     */
    public boolean isPending() {
        return joinStatus != null && joinStatus == 0;
    }

    /**
     * 是否已退出
     */
    public boolean hasLeft() {
        return joinStatus != null && joinStatus == 2;
    }

    /**
     * 设置为群主
     */
    public void setAsOwner() {
        this.role = 2;
    }

    /**
     * 设置为管理员
     */
    public void setAsAdministrator() {
        this.role = 1;
    }

    /**
     * 设置为普通成员
     */
    public void setAsMember() {
        this.role = 0;
    }

    /**
     * 确认加入
     */
    public void confirmJoin() {
        this.joinStatus = 1;
        this.joinedAt = LocalDateTime.now();
    }

    /**
     * 退出群组
     */
    public void leaveGroup() {
        this.joinStatus = 2;
        this.leftAt = LocalDateTime.now();
    }
}