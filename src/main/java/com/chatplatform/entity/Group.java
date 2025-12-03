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
import java.util.List;

/**
 * 群组实体类
 */
@Entity
@Table(name = "chat_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 群号
     */
    @Column(name = "group_number", unique = true, nullable = false, length = 20)
    private String groupNumber;

    /**
     * 群名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 群描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 群头像
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 群主ID
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 最大成员数
     */
    @Column(name = "max_members")
    private Integer maxMembers = 500;

    /**
     * 入群审批 0:自由加入 1:需要审批
     */
    @Column(name = "join_approval", nullable = false)
    private Integer joinApproval = 1;

    /**
     * 群状态 0:已解散 1:正常
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

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
     * 群主信息（用于查询时关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    @JsonIgnore
    private User owner;

    /**
     * 群成员列表
     */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<GroupMember> members;

    /**
     * 获取入群审批描述
     */
    public String getJoinApprovalDescription() {
        if (joinApproval == null) return "需要审批";
        switch (joinApproval) {
            case 0: return "自由加入";
            case 1: return "需要审批";
            default: return "需要审批";
        }
    }

    /**
     * 获取群状态描述
     */
    public String getStatusDescription() {
        if (status == null) return "已解散";
        switch (status) {
            case 0: return "已解散";
            case 1: return "正常";
            default: return "已解散";
        }
    }

    /**
     * 群组是否正常
     */
    public boolean isActive() {
        return status != null && status == 1;
    }

    /**
     * 是否需要入群审批
     */
    public boolean needsApproval() {
        return joinApproval != null && joinApproval == 1;
    }

    /**
     * 解散群组
     */
    public void dissolve() {
        this.status = 0;
    }

    /**
     * 获取当前成员数量
     */
    public Integer getCurrentMemberCount() {
        return members != null ?
            (int) members.stream().filter(member -> member.getJoinStatus() == 1).count() : 0;
    }

    /**
     * 是否已达到最大成员数
     */
    public boolean isFull() {
        return getCurrentMemberCount() >= maxMembers;
    }

    // 获取群成员数量的别名方法
    public Integer getMemberCount() {
        return getCurrentMemberCount();
    }
}