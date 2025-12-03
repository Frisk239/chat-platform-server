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
 * 好友关系实体类
 */
@Entity
@Table(name = "friendships")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 好友ID
     */
    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    /**
     * 关系状态 0:待确认 1:已确认 2:已拒绝
     */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    /**
     * 申请消息
     */
    @Column(name = "request_message", length = 200)
    private String requestMessage;

    /**
     * 好友备注
     */
    @Column(name = "remark", length = 100)
    private String remark;

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
     * 好友用户信息（用于查询时关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", insertable = false, updatable = false)
    @JsonIgnore
    private User friend;

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待确认";
            case 1: return "已确认";
            case 2: return "已拒绝";
            default: return "未知";
        }
    }

    /**
     * 是否为好友关系
     */
    public boolean isFriend() {
        return status != null && status == 1;
    }

    /**
     * 是否为待确认状态
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
     * 确认好友关系
     */
    public void confirm() {
        this.status = 1;
    }

    /**
     * 拒绝好友申请
     */
    public void reject() {
        this.status = 2;
    }
}