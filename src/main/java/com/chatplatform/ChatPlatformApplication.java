package com.chatplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 聊天平台应用启动类
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ChatPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatPlatformApplication.class, args);
    }
}