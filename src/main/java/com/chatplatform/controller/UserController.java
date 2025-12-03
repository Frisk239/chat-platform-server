package com.chatplatform.controller;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理", description = "用户信息管理相关接口")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(HttpServletRequest request) {
        try {
            String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            UserResponse userResponse = userService.getUserById(userId);
            if (userResponse == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("获取用户信息失败"));
        }
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/profile")
    @Operation(summary = "更新当前用户信息", description = "更新当前登录用户的基本信息")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UserService.UserUpdateRequest updateRequest,
            HttpServletRequest request) {
        try {
            String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            UserResponse userResponse = userService.updateUser(userId, updateRequest);
            return ResponseEntity.ok(ApiResponse.success("更新成功", userResponse));
        } catch (RuntimeException e) {
            log.error("更新用户信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前用户的登录密码")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Parameter(description = "旧密码", required = true)
            @RequestParam String oldPassword,
            @Parameter(description = "新密码", required = true)
            @RequestParam String newPassword,
            HttpServletRequest request) {
        try {
            String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            userService.changePassword(userId, oldPassword, newPassword);
            return ResponseEntity.ok(ApiResponse.success("密码修改成功"));
        } catch (RuntimeException e) {
            log.error("修改密码失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{userId}")
    @Operation(summary = "根据ID获取用户信息", description = "根据用户ID获取用户基本信息")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        try {
            UserResponse userResponse = userService.getUserById(userId);
            if (userResponse == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (Exception e) {
            log.error("获取用户信息失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("获取用户信息失败"));
        }
    }

    /**
     * 根据QQ号获取用户信息
     */
    @GetMapping("/qq/{qqNumber}")
    @Operation(summary = "根据QQ号获取用户信息", description = "根据QQ号获取用户基本信息")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByQqNumber(
            @Parameter(description = "QQ号", required = true)
            @PathVariable String qqNumber) {
        try {
            UserResponse userResponse = userService.getUserByQqNumber(qqNumber);
            if (userResponse == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (Exception e) {
            log.error("获取用户信息失败: qqNumber={}", qqNumber, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("获取用户信息失败"));
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "根据关键词搜索用户（昵称、QQ号等）")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @Parameter(description = "搜索关键词", required = true)
            @RequestParam String keyword,
            @Parameter(description = "页码，从0开始", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
            Long currentUserId = jwtTokenProvider.getUserIdFromToken(token);

            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<UserResponse> users = userService.searchUsers(keyword, currentUserId, pageable);

            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("搜索用户失败: keyword={}", keyword, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("搜索用户失败"));
        }
    }

    /**
     * 根据ID列表获取用户信息
     */
    @PostMapping("/batch")
    @Operation(summary = "批量获取用户信息", description = "根据用户ID列表批量获取用户信息")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByIds(
            @Parameter(description = "用户ID列表", required = true)
            @RequestBody List<Long> userIds) {
        try {
            List<UserResponse> users = userService.getUsersByIds(userIds);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("批量获取用户信息失败: userIds={}", userIds, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("批量获取用户信息失败"));
        }
    }

    /**
     * 更新用户在线状态
     */
    @PutMapping("/status")
    @Operation(summary = "更新用户在线状态", description = "更新当前用户的在线状态")
    public ResponseEntity<ApiResponse<String>> updateUserStatus(
            @Parameter(description = "在线状态 0:离线 1:在线 2:忙碌 3:隐身", required = true)
            @RequestParam Integer status,
            HttpServletRequest request) {
        Long userId = null;
        try {
            String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
            userId = jwtTokenProvider.getUserIdFromToken(token);

            userService.updateUserStatus(userId, status);
            return ResponseEntity.ok(ApiResponse.success("状态更新成功"));
        } catch (Exception e) {
            log.error("更新用户状态失败: userId={}, status={}", userId, status, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("更新用户状态失败"));
        }
    }

    /**
     * 用户退出登录（更新状态为离线）
     */
    @PostMapping("/offline")
    @Operation(summary = "用户下线", description = "将当前用户状态设置为离线")
    public ResponseEntity<ApiResponse<String>> setOffline(HttpServletRequest request) {
        try {
            String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            userService.updateUserStatus(userId, 0); // 设置为离线状态
            return ResponseEntity.ok(ApiResponse.success("已下线"));
        } catch (Exception e) {
            log.error("用户下线失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("用户下线失败"));
        }
    }

    /**
     * 用户上线（更新状态为在线）
     */
    @PostMapping("/online")
    @Operation(summary = "用户上线", description = "将当前用户状态设置为在线")
    public ResponseEntity<ApiResponse<String>> setOnline(HttpServletRequest request) {
        try {
            String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            userService.updateUserStatus(userId, 1); // 设置为在线状态
            return ResponseEntity.ok(ApiResponse.success("已上线"));
        } catch (Exception e) {
            log.error("用户上线失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("用户上线失败"));
        }
    }
}