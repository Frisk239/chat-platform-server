package com.chatplatform.controller;

import com.chatplatform.dto.request.FriendActionRequest;
import com.chatplatform.dto.request.FriendRequest;
import com.chatplatform.dto.response.ApiResponse;
import com.chatplatform.dto.response.FriendResponse;
import com.chatplatform.service.FriendshipService;
import com.chatplatform.service.FriendshipService.FriendshipStats;
import com.chatplatform.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友管理控制器
 */
@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "好友管理", description = "好友关系管理相关接口")
public class FriendController {

    private final FriendshipService friendshipService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 发送好友申请
     */
    @PostMapping("/request")
    @Operation(summary = "发送好友申请", description = "向指定用户发送好友申请")
    public ResponseEntity<ApiResponse<FriendResponse>> sendFriendRequest(
            @Valid @RequestBody FriendRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            FriendResponse response = friendshipService.sendFriendRequest(userId, request);
            return ResponseEntity.ok(ApiResponse.success("好友申请发送成功", response));
        } catch (Exception e) {
            log.error("发送好友申请失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 处理好友申请（接受或拒绝）
     */
    @PostMapping("/handle")
    @Operation(summary = "处理好友申请", description = "接受或拒绝好友申请")
    public ResponseEntity<ApiResponse<FriendResponse>> handleFriendRequest(
            @Valid @RequestBody FriendActionRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            FriendResponse response = friendshipService.handleFriendRequest(userId, request);
            return ResponseEntity.ok(ApiResponse.success("好友申请处理成功", response));
        } catch (Exception e) {
            log.error("处理好友申请失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取好友列表", description = "获取当前用户的好友列表")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getFriendList(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<FriendResponse> friends = friendshipService.getFriendList(userId);
            return ResponseEntity.ok(ApiResponse.success(friends));
        } catch (Exception e) {
            log.error("获取好友列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取好友列表（分页）
     */
    @GetMapping("/list/page")
    @Operation(summary = "获取好友列表（分页）", description = "分页获取好友列表")
    public ResponseEntity<ApiResponse<Page<FriendResponse>>> getFriendListPage(
            @Parameter(description = "页码，从0开始", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            Page<FriendResponse> friendPage = friendshipService.getFriendList(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success(friendPage));
        } catch (Exception e) {
            log.error("获取好友列表（分页）失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取收到的好友申请
     */
    @GetMapping("/requests")
    @Operation(summary = "获取好友申请", description = "获取收到的好友申请列表")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getFriendRequests(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<FriendResponse> requests = friendshipService.getFriendRequests(userId);
            return ResponseEntity.ok(ApiResponse.success(requests));
        } catch (Exception e) {
            log.error("获取好友申请列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    @Operation(summary = "删除好友", description = "删除指定好友")
    public ResponseEntity<ApiResponse<String>> deleteFriend(
            @Parameter(description = "好友ID", required = true)
            @PathVariable Long friendId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            friendshipService.deleteFriend(userId, friendId);
            return ResponseEntity.ok(ApiResponse.success("好友已删除"));
        } catch (Exception e) {
            log.error("删除好友失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 更新好友备注
     */
    @PutMapping("/{friendId}/remark")
    @Operation(summary = "更新好友备注", description = "更新指定好友的备注信息")
    public ResponseEntity<ApiResponse<FriendResponse>> updateFriendRemark(
            @Parameter(description = "好友ID", required = true)
            @PathVariable Long friendId,
            @Parameter(description = "备注信息", required = true)
            @RequestParam String remark,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            FriendResponse response = friendshipService.updateFriendRemark(userId, friendId, remark);
            return ResponseEntity.ok(ApiResponse.success("好友备注更新成功", response));
        } catch (Exception e) {
            log.error("更新好友备注失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 搜索好友
     */
    @GetMapping("/search")
    @Operation(summary = "搜索好友", description = "根据关键词搜索好友")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> searchFriends(
            @Parameter(description = "搜索关键词", required = true)
            @RequestParam String keyword,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<FriendResponse> friends = friendshipService.searchFriends(userId, keyword);
            return ResponseEntity.ok(ApiResponse.success(friends));
        } catch (Exception e) {
            log.error("搜索好友失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 检查是否为好友
     */
    @GetMapping("/check/{targetUserId}")
    @Operation(summary = "检查好友关系", description = "检查与指定用户是否为好友关系")
    public ResponseEntity<ApiResponse<Boolean>> checkFriendship(
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable Long targetUserId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            boolean areFriends = friendshipService.areFriends(userId, targetUserId);
            return ResponseEntity.ok(ApiResponse.success(areFriends));
        } catch (Exception e) {
            log.error("检查好友关系失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取好友统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取好友统计", description = "获取好友关系统计信息")
    public ResponseEntity<ApiResponse<FriendshipStats>> getFriendStats(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            FriendshipStats stats = friendshipService.getFriendStats(userId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取好友统计失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取推荐好友
     */
    @GetMapping("/recommended")
    @Operation(summary = "获取推荐好友", description = "获取可能认识的好友推荐")
    public ResponseEntity<ApiResponse<List<Object>>> getRecommendedFriends(
            @Parameter(description = "推荐数量", example = "5")
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<Object> recommendedFriends = friendshipService.getRecommendedFriends(userId, limit)
                    .stream()
                    .map(user -> Map.of(
                            "id", user.getId(),
                            "qqNumber", user.getQqNumber(),
                            "nickname", user.getNickname(),
                            "avatarUrl", user.getAvatarUrl(),
                            "status", user.getStatus()
                    ))
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(recommendedFriends));
        } catch (Exception e) {
            log.error("获取推荐好友失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 从Token中获取用户ID
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}