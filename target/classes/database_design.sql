-- 聊天平台数据库设计
-- 创建数据库
CREATE DATABASE IF NOT EXISTS chat_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE chat_platform;

-- 用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    qq_number VARCHAR(20) UNIQUE NOT NULL COMMENT 'QQ号',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    nickname VARCHAR(100) NOT NULL COMMENT '昵称',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    signature VARCHAR(200) COMMENT '个性签名',
    gender TINYINT DEFAULT 0 COMMENT '性别 0:未知 1:男 2:女',
    birthday DATE COMMENT '生日',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 0 COMMENT '在线状态 0:离线 1:在线 2:忙碌 3:隐身',
    last_login_time DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_qq_number (qq_number),
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 好友关系表
CREATE TABLE friendships (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    status TINYINT DEFAULT 0 COMMENT '关系状态 0:待确认 1:已确认 2:已拒绝',
    request_message VARCHAR(200) COMMENT '申请消息',
    remark VARCHAR(100) COMMENT '好友备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_friendship (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,
    CHECK (user_id != friend_id),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_id (friend_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 群组表
CREATE TABLE groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群组ID',
    group_number VARCHAR(20) UNIQUE NOT NULL COMMENT '群号',
    name VARCHAR(100) NOT NULL COMMENT '群名称',
    description TEXT COMMENT '群描述',
    avatar_url VARCHAR(500) COMMENT '群头像',
    owner_id BIGINT NOT NULL COMMENT '群主ID',
    max_members INT DEFAULT 500 COMMENT '最大成员数',
    join_approval TINYINT DEFAULT 1 COMMENT '入群审批 0:自由加入 1:需要审批',
    status TINYINT DEFAULT 1 COMMENT '群状态 0:已解散 1:正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_group_number (group_number),
    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 群成员表
CREATE TABLE group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员关系ID',
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role TINYINT DEFAULT 0 COMMENT '角色 0:普通成员 1:管理员 2:群主',
    nickname VARCHAR(100) COMMENT '群昵称',
    join_status TINYINT DEFAULT 1 COMMENT '加入状态 0:待审核 1:已加入 2:已退出',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    left_at DATETIME COMMENT '离开时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_group_member (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role (role),
    INDEX idx_join_status (join_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- 消息表
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT COMMENT '接收者ID（私聊消息）',
    group_id BIGINT COMMENT '群组ID（群聊消息）',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type TINYINT DEFAULT 0 COMMENT '消息类型 0:文本 1:图片 2:文件 3:语音 4:视频 5:表情',
    status TINYINT DEFAULT 0 COMMENT '消息状态 0:已发送 1:已送达 2:已读',
    reply_to_id BIGINT COMMENT '回复的消息ID',
    is_revoked TINYINT DEFAULT 0 COMMENT '是否撤回 0:否 1:是',
    revoke_time DATETIME COMMENT '撤回时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to_id) REFERENCES messages(id) ON DELETE SET NULL,
    CHECK (receiver_id IS NOT NULL OR group_id IS NOT NULL),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_group_id (group_id),
    INDEX idx_created_at (created_at),
    INDEX idx_chat_private (receiver_id, created_at),
    INDEX idx_chat_group (group_id, created_at),
    INDEX idx_status (status),
    INDEX idx_message_type (message_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 消息附件表
CREATE TABLE message_attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    original_name VARCHAR(255) COMMENT '原文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    INDEX idx_message_id (message_id),
    INDEX idx_file_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息附件表';

-- 消息已读状态表
CREATE TABLE message_read_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '已读状态ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    read_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_message_user_read (message_id, user_id),
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_message_id (message_id),
    INDEX idx_user_id (user_id),
    INDEX idx_read_time (read_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读状态表';

-- 用户会话表（WebSocket连接管理）
CREATE TABLE user_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT UNIQUE NOT NULL COMMENT '用户ID',
    session_id VARCHAR(255) NOT NULL COMMENT '会话ID',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    device_info VARCHAR(200) COMMENT '设备信息',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    status TINYINT DEFAULT 1 COMMENT '会话状态 0:离线 1:在线',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_status (status),
    INDEX idx_last_active_time (last_active_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会话表';

-- 创建视图：用户好友列表
CREATE VIEW v_user_friends AS
SELECT
    u.id as user_id,
    f.friend_id,
    u.nickname as user_nickname,
    u2.nickname as friend_nickname,
    u2.qq_number as friend_qq_number,
    u2.avatar_url as friend_avatar_url,
    u2.status as friend_status,
    f.remark,
    f.created_at as friendship_time
FROM users u
JOIN friendships f ON u.id = f.user_id
JOIN users u2 ON f.friend_id = u2.id
WHERE f.status = 1;

-- 创建视图：群组信息
CREATE VIEW v_group_info AS
SELECT
    g.id as group_id,
    g.group_number,
    g.name as group_name,
    g.description,
    g.avatar_url as group_avatar,
    g.owner_id,
    u.nickname as owner_nickname,
    u.qq_number as owner_qq_number,
    COUNT(gm.user_id) as member_count,
    g.max_members,
    g.join_approval,
    g.status,
    g.created_at
FROM groups g
LEFT JOIN users u ON g.owner_id = u.id
LEFT JOIN group_members gm ON g.id = gm.group_id AND gm.join_status = 1
WHERE g.status = 1
GROUP BY g.id;

-- 插入初始化数据（测试用）
INSERT INTO users (qq_number, username, nickname, password, email, status) VALUES
('10000001', 'admin', '系统管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKiVpTjHNlXqLQr3g/wpkf82W', 'admin@chat.com', 1),
('10000002', 'testuser1', '测试用户1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKiVpTjHNlXqLQr3g/wpkf82W', 'test1@chat.com', 0),
('10000003', 'testuser2', '测试用户2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKiVpTjHNlXqLQr3g/wpkf82W', 'test2@chat.com', 0);

-- 创建群组（测试用）
INSERT INTO groups (group_number, name, description, owner_id) VALUES
('80000001', '技术交流群', '技术讨论和交流', 1),
('80000002', '朋友聊天群', '朋友们的聊天群', 2);

-- 添加群成员（测试用）
INSERT INTO group_members (group_id, user_id, role) VALUES
(1, 1, 2),  -- 群主
(1, 2, 0),  -- 普通成员
(1, 3, 0),  -- 普通成员
(2, 2, 2),  -- 群主
(2, 1, 0);  -- 普通成员

-- 添加好友关系（测试用）
INSERT INTO friendships (user_id, friend_id, status, request_message) VALUES
(1, 2, 1, '我是系统管理员，想加你为好友'),
(2, 1, 1, '我想加你为好友'),
(2, 3, 1, '你好，认识一下');

-- 创建索引优化查询性能
-- 为消息表添加复合索引，优化聊天记录查询
CREATE INDEX idx_message_conversation_private ON messages(receiver_id, sender_id, created_at);
CREATE INDEX idx_message_conversation_group ON messages(group_id, created_at DESC);

-- 为用户表添加状态和时间索引，优化在线用户查询
CREATE INDEX idx_user_status_time ON users(status, last_login_time);

-- 为群成员表添加复合索引，优化群成员查询
CREATE INDEX idx_group_member_role_status ON group_members(group_id, role, join_status);