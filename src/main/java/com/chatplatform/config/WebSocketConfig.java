package com.chatplatform.config;

import com.chatplatform.websocket.ChatHandler;
import com.chatplatform.websocket.CustomHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatHandler chatHandler;
    private final CustomHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws/{userId}")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*") // 在生产环境中应该设置具体的域名
                .withSockJS(); // 启用SockJS支持，提供浏览器兼容性
    }
}