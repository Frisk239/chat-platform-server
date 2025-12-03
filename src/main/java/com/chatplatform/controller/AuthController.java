package com.chatplatform.controller;

import com.chatplatform.dto.request.UserLoginRequest;
import com.chatplatform.dto.request.UserRegisterRequest;
import com.chatplatform.dto.response.ApiResponse;
import com.chatplatform.dto.response.UserResponse;
import com.chatplatform.service.UserService;
import com.chatplatform.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "认证管理", description = "用户注册、登录、Token刷新等认证相关接口")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账号")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegisterRequest registerRequest) {
        log.info("用户注册请求: username={}", registerRequest.getUsername());

        try {
            UserResponse userResponse = userService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("注册成功", userResponse));
        } catch (RuntimeException e) {
            log.error("用户注册失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录获取访问令牌")
    public ResponseEntity<ApiResponse<UserService.LoginResult>> login(
            @Valid @RequestBody UserLoginRequest loginRequest) {
        log.info("用户登录请求: {}", loginRequest.getUsernameOrQqNumber());

        try {
            UserService.LoginResult loginResult = userService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("登录成功", loginResult));
        } catch (RuntimeException e) {
            log.error("用户登录失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用刷新Token获取新的访问令牌")
    public ResponseEntity<ApiResponse<JwtTokenProvider.TokenInfo>> refreshToken(
            @Parameter(description = "刷新Token", required = true)
            @RequestParam String refreshToken) {
        log.info("刷新Token请求");

        try {
            JwtTokenProvider.TokenInfo tokenInfo = userService.refreshToken(refreshToken);
            return ResponseEntity.ok(ApiResponse.success("Token刷新成功", tokenInfo));
        } catch (RuntimeException e) {
            log.error("刷新Token失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 验证Token
     */
    @GetMapping("/verify-token")
    @Operation(summary = "验证Token", description = "验证Token是否有效")
    public ResponseEntity<ApiResponse<Boolean>> verifyToken(HttpServletRequest request) {
        String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));

        if (token == null) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }

        try {
            boolean isValid = jwtTokenProvider.validateToken(token);
            return ResponseEntity.ok(ApiResponse.success(isValid));
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    /**
     * 获取Token信息
     */
    @GetMapping("/token-info")
    @Operation(summary = "获取Token信息", description = "获取当前Token的详细信息")
    public ResponseEntity<ApiResponse<TokenInfoResponse>> getTokenInfo(HttpServletRequest request) {
        String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));

        if (token == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Token不存在"));
        }

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("Token无效"));
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            String tokenType = jwtTokenProvider.getTokenTypeFromToken(token);
            boolean isExpired = jwtTokenProvider.isTokenExpired(token);
            boolean isExpiringSoon = jwtTokenProvider.isTokenExpiringSoon(token);

            TokenInfoResponse tokenInfo = TokenInfoResponse.builder()
                    .userId(userId)
                    .username(username)
                    .tokenType(tokenType)
                    .isExpired(isExpired)
                    .isExpiringSoon(isExpiringSoon)
                    .isValid(!isExpired)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(tokenInfo));

        } catch (Exception e) {
            log.error("获取Token信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("获取Token信息失败"));
        }
    }

    /**
     * 登出（标记Token为无效）
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出，标记当前Token为无效")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));

        if (token == null) {
            return ResponseEntity.ok(ApiResponse.success("登出成功"));
        }

        log.info("用户登出请求");

        try {
            // 这里可以实现Token黑名单机制
            // 目前简单返回成功，客户端删除Token即可
            return ResponseEntity.ok(ApiResponse.success("登出成功"));
        } catch (Exception e) {
            log.error("用户登出失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("登出失败"));
        }
    }

    /**
     * Token信息响应DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TokenInfoResponse {
        private Long userId;
        private String username;
        private String tokenType;
        private Boolean isValid;
        private Boolean isExpired;
        private Boolean isExpiringSoon;
    }
}