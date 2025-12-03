package com.chatplatform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器
 */
@Component
@Slf4j
public class CustomHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        log.debug("WebSocket握手开始，请求URI: {}", request.getURI());

        // 从URL路径中获取userId
        String path = request.getURI().getPath();
        if (path != null && path.contains("/ws/")) {
            String userId = path.substring(path.lastIndexOf("/") + 1);

            try {
                Long.parseLong(userId);
                attributes.put("userId", userId);
                log.debug("WebSocket握手，用户ID: {}", userId);
                return true;
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID: {}", userId);
                return false;
            }
        }

        log.warn("无法从URL中提取用户ID: {}", path);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                              ServerHttpResponse response,
                              WebSocketHandler wsHandler,
                              Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手失败", exception);
        } else {
            log.debug("WebSocket握手成功完成");
        }
    }
}