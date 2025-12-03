package com.chatplatform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * WebSocket会话管理器
 */
@Component
@Slf4j
public class WebSocketSessionManager {

    /**
     * 用户ID到WebSocket会话的映射
     */
    private final ConcurrentMap<Long, WebSocketSessionInfo> userSessions = new ConcurrentHashMap<>();

    /**
     * 添加WebSocket会话
     */
    public void addSession(Long userId, String sessionId, org.springframework.web.socket.WebSocketSession session) {
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.builder()
                .userId(userId)
                .sessionId(sessionId)
                .session(session)
                .connectTime(System.currentTimeMillis())
                .build();

        userSessions.put(userId, sessionInfo);
        log.info("用户 {} 建立WebSocket连接，会话ID: {}", userId, sessionId);

        // 更新用户在线状态
        updateUserOnlineStatus(userId, true);
    }

    /**
     * 移除WebSocket会话
     */
    public void removeSession(Long userId) {
        WebSocketSessionInfo sessionInfo = userSessions.remove(userId);
        if (sessionInfo != null) {
            log.info("用户 {} 断开WebSocket连接，会话ID: {}", userId, sessionInfo.getSessionId());

            // 更新用户离线状态
            updateUserOnlineStatus(userId, false);
        }
    }

    /**
     * 获取用户的WebSocket会话
     */
    public WebSocketSessionInfo getSession(Long userId) {
        return userSessions.get(userId);
    }

    /**
     * 获取用户的WebSocket Session
     */
    public org.springframework.web.socket.WebSocketSession getWebSocketSession(Long userId) {
        WebSocketSessionInfo sessionInfo = userSessions.get(userId);
        return sessionInfo != null ? sessionInfo.getSession() : null;
    }

    /**
     * 向指定用户发送消息
     */
    public boolean sendToUser(Long userId, Object message) {
        WebSocketSessionInfo sessionInfo = userSessions.get(userId);
        if (sessionInfo == null) {
            log.warn("用户 {} 不在线，无法发送消息", userId);
            return false;
        }

        try {
            org.springframework.web.socket.WebSocketSession session = sessionInfo.getSession();
            if (session != null && session.isOpen()) {
                session.sendMessage(new org.springframework.web.socket.TextMessage(
                        com.alibaba.fastjson2.JSON.toJSONString(message)));
                log.debug("向用户 {} 发送消息成功", userId);
                return true;
            } else {
                log.warn("用户 {} 的WebSocket会话已关闭", userId);
                removeSession(userId);
                return false;
            }
        } catch (Exception e) {
            log.error("向用户 {} 发送消息失败", userId, e);
            // 发送失败时移除会话
            removeSession(userId);
            return false;
        }
    }

    /**
     * 向多个用户批量发送消息
     */
    public void sendToUsers(java.util.List<Long> userIds, Object message) {
        userIds.parallelStream().forEach(userId -> sendToUser(userId, message));
    }

    /**
     * 向所有在线用户发送消息
     */
    public void broadcast(Object message) {
        userSessions.keySet().parallelStream().forEach(userId -> sendToUser(userId, message));
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId);
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    /**
     * 获取所有在线用户ID
     */
    public java.util.Set<Long> getOnlineUserIds() {
        return new java.util.HashSet<>(userSessions.keySet());
    }

    /**
     * 获取所有在线用户的会话信息
     */
    public Map<Long, WebSocketSessionInfo> getAllSessions() {
        return new java.util.HashMap<>(userSessions);
    }

    /**
     * 清理所有会话
     */
    public void clearAllSessions() {
        log.info("清理所有WebSocket会话，当前在线用户数: {}", userSessions.size());
        userSessions.clear();
    }

    /**
     * 检查并清理无效会话
     */
    public void cleanInvalidSessions() {
        userSessions.entrySet().removeIf(entry -> {
            try {
                org.springframework.web.socket.WebSocketSession session = entry.getValue().getSession();
                return session == null || !session.isOpen();
            } catch (Exception e) {
                log.debug("检查会话时发生异常，移除会话: userId={}, error={}", entry.getKey(), e.getMessage());
                return true;
            }
        });
    }

    /**
     * 更新用户在线状态
     */
    private void updateUserOnlineStatus(Long userId, boolean isOnline) {
        try {
            // 这里可以调用UserService来更新用户的在线状态
            // 暂时只记录日志
            log.debug("更新用户 {} 在线状态: {}", userId, isOnline ? "在线" : "离线");
        } catch (Exception e) {
            log.error("更新用户在线状态失败: userId={}, isOnline={}", userId, isOnline, e);
        }
    }

    /**
     * 获取会话统计信息
     */
    public SessionStats getSessionStats() {
        return SessionStats.builder()
                .totalSessions(userSessions.size())
                .build();
    }

    /**
     * WebSocket会话信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WebSocketSessionInfo {
        private Long userId;
        private String sessionId;
        private org.springframework.web.socket.WebSocketSession session;
        private Long connectTime;
        private Long lastHeartbeat;
    }

    /**
     * 会话统计信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SessionStats {
        private Integer totalSessions;
    }
}