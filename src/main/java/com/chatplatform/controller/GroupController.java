package com.chatplatform.controller;

import com.chatplatform.dto.request.GroupCreateRequest;
import com.chatplatform.dto.request.GroupJoinRequest;
import com.chatplatform.dto.request.GroupMemberManageRequest;
import com.chatplatform.dto.request.GroupUpdateRequest;
import com.chatplatform.dto.response.ApiResponse;
import com.chatplatform.dto.response.GroupResponse;
import com.chatplatform.service.GroupService;
import com.chatplatform.util.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 群组管理控制器
 */
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "群组管理", description = "群组管理相关接口")
public class GroupController {

    private final GroupService groupService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 创建群组
     */
    @PostMapping
    @Operation(summary = "创建群组", description = "创建新的群组")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody GroupCreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            GroupResponse response = groupService.createGroup(userId, request);
            return ResponseEntity.ok(ApiResponse.success("群组创建成功", response));
        } catch (Exception e) {
            log.error("创建群组失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 申请加入群组
     */
    @PostMapping("/join")
    @Operation(summary = "申请加入群组", description = "用户申请加入指定群组")
    public ResponseEntity<ApiResponse<GroupResponse>> joinGroup(
            @Valid @RequestBody GroupJoinRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            GroupResponse response = groupService.joinGroup(userId, request);
            return ResponseEntity.ok(ApiResponse.success("加群申请处理成功", response));
        } catch (Exception e) {
            log.error("申请加入群组失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 通过群号加入群组
     */
    @PostMapping("/join-by-number")
    @Operation(summary = "通过群号加入群组", description = "用户通过群号申请加入群组")
    public ResponseEntity<ApiResponse<GroupResponse>> joinGroupByNumber(
            @Parameter(description = "群号", required = true)
            @RequestParam String groupNumber,
            @Parameter(description = "申请消息", required = false)
            @RequestParam(required = false) String message,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            GroupJoinRequest request = GroupJoinRequest.builder()
                    .groupNumber(groupNumber)
                    .message(message)
                    .build();
            GroupResponse response = groupService.joinGroup(userId, request);
            return ResponseEntity.ok(ApiResponse.success("加群申请处理成功", response));
        } catch (Exception e) {
            log.error("通过群号加入群组失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 管理群成员
     */
    @PostMapping("/members/manage")
    @Operation(summary = "管理群成员", description = "管理员或群主管理群成员")
    public ResponseEntity<ApiResponse<String>> manageGroupMembers(
            @Valid @RequestBody GroupMemberManageRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            groupService.manageGroupMembers(userId, request);
            return ResponseEntity.ok(ApiResponse.success("群成员管理操作成功"));
        } catch (Exception e) {
            log.error("管理群成员失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 退出群组
     */
    @PostMapping("/leave/{groupId}")
    @Operation(summary = "退出群组", description = "用户退出指定群组")
    public ResponseEntity<ApiResponse<String>> leaveGroup(
            @Parameter(description = "群组ID", required = true)
            @PathVariable Long groupId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            groupService.leaveGroup(userId, groupId);
            return ResponseEntity.ok(ApiResponse.success("已退出群组"));
        } catch (Exception e) {
            log.error("退出群组失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取群组详情
     */
    @GetMapping("/{groupId}")
    @Operation(summary = "获取群组详情", description = "获取指定群组的详细信息")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupDetail(
            @Parameter(description = "群组ID", required = true)
            @PathVariable Long groupId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            GroupResponse response = groupService.getGroupDetail(groupId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("获取群组详情失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 更新群组信息
     */
    @PutMapping("/{groupId}")
    @Operation(summary = "更新群组信息", description = "群主或管理员更新群组信息")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @Parameter(description = "群组ID", required = true)
            @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            GroupResponse response = groupService.updateGroup(userId, groupId, request);
            return ResponseEntity.ok(ApiResponse.success("群组信息更新成功", response));
        } catch (Exception e) {
            log.error("更新群组信息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 解散群组
     */
    @DeleteMapping("/{groupId}")
    @Operation(summary = "解散群组", description = "群主解散群组")
    public ResponseEntity<ApiResponse<String>> dissolveGroup(
            @Parameter(description = "群组ID", required = true)
            @PathVariable Long groupId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            groupService.dissolveGroup(userId, groupId);
            return ResponseEntity.ok(ApiResponse.success("群组已解散"));
        } catch (Exception e) {
            log.error("解散群组失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 转让群主
     */
    @PostMapping("/{groupId}/transfer")
    @Operation(summary = "转让群主", description = "当前群主将群主权限转让给其他成员")
    public ResponseEntity<ApiResponse<String>> transferOwnership(
            @Parameter(description = "群组ID", required = true)
            @PathVariable Long groupId,
            @Parameter(description = "新群主用户ID", required = true)
            @RequestParam Long newOwnerId,
            HttpServletRequest httpRequest) {
        try {
            Long currentOwnerId = getUserIdFromToken(httpRequest);
            groupService.transferOwnership(currentOwnerId, groupId, newOwnerId);
            return ResponseEntity.ok(ApiResponse.success("群主转让成功"));
        } catch (Exception e) {
            log.error("转让群主失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取用户的群组列表
     */
    @GetMapping("/my")
    @Operation(summary = "获取用户群组列表", description = "获取当前用户加入的所有群组")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getUserGroups(
            @Parameter(description = "群组状态筛选", example = "1")
            @RequestParam(required = false) Integer status,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            Page<GroupResponse> groups = groupService.getUserGroups(userId, 0, 20, status);
            return ResponseEntity.ok(ApiResponse.success(groups));
        } catch (Exception e) {
            log.error("获取用户群组列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 分页获取用户的群组列表
     */
    @GetMapping("/my/page")
    @Operation(summary = "分页获取用户群组列表", description = "分页获取当前用户加入的群组")
    public ResponseEntity<ApiResponse<Page<GroupResponse>>> getUserGroupsPage(
            @Parameter(description = "页码，从0开始", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "群组状态筛选", example = "1")
            @RequestParam(required = false) Integer status,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            Page<GroupResponse> groupPage = groupService.getUserGroups(userId, page, size, status);
            return ResponseEntity.ok(ApiResponse.success(groupPage));
        } catch (Exception e) {
            log.error("分页获取用户群组列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取用户创建的群组
     */
    @GetMapping("/created")
    @Operation(summary = "获取用户创建的群组", description = "获取当前用户创建的所有群组")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getCreatedGroups(
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<GroupResponse> groups = groupService.getCreatedGroups(userId);
            return ResponseEntity.ok(ApiResponse.success(groups));
        } catch (Exception e) {
            log.error("获取用户创建的群组失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取用户管理的群组
     */
    @GetMapping("/managed")
    @Operation(summary = "获取用户管理的群组", description = "获取当前用户管理的群组（包括创建的和担任管理员的）")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getManagedGroups(
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<GroupResponse> groups = groupService.getManagedGroups(userId);
            return ResponseEntity.ok(ApiResponse.success(groups));
        } catch (Exception e) {
            log.error("获取用户管理的群组失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 搜索群组
     */
    @GetMapping("/search")
    @Operation(summary = "搜索群组", description = "根据关键词搜索群组")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> searchGroups(
            @Parameter(description = "搜索关键词", required = true)
            @RequestParam String keyword,
            @Parameter(description = "搜索类型", example = "name")
            @RequestParam(defaultValue = "name") String searchType,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<GroupResponse> groups = groupService.searchGroups(userId, keyword, searchType);
            return ResponseEntity.ok(ApiResponse.success(groups));
        } catch (Exception e) {
            log.error("搜索群组失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 获取推荐群组
     */
    @GetMapping("/recommended")
    @Operation(summary = "获取推荐群组", description = "获取推荐的群组")
    public ResponseEntity<ApiResponse<List<Object>>> getRecommendedGroups(
            @Parameter(description = "推荐数量", example = "5")
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<Object> recommendedGroups = groupService.getRecommendedGroups(userId, limit)
                    .stream()
                    .map(group -> Map.of(
                            "id", group.getId(),
                            "groupNumber", group.getGroupNumber(),
                            "name", group.getName(),
                            "description", group.getDescription(),
                            "avatarUrl", group.getAvatarUrl(),
                            "memberCount", group.getMemberCount(),
                            "maxMembers", group.getMaxMembers(),
                            "joinApproval", group.getJoinApproval()
                    ))
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(recommendedGroups));
        } catch (Exception e) {
            log.error("获取推荐群组失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 设置管理员
     */
    @PostMapping("/{groupId}/set-admin")
    @Operation(summary = "设置管理员", description = "群主设置群管理员")
    public ResponseEntity<ApiResponse<String>> setGroupAdmin(
            @Parameter(description = "群组ID", required = true)
            @PathVariable Long groupId,
            @Parameter(description = "用户ID", required = true)
            @RequestParam Long userId,
            HttpServletRequest httpRequest) {
        try {
            Long ownerId = getUserIdFromToken(httpRequest);
            groupService.setGroupAdmin(ownerId, groupId, userId);
            return ResponseEntity.ok(ApiResponse.success("管理员设置成功"));
        } catch (Exception e) {
            log.error("设置管理员失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 取消管理员
     */
    @PostMapping("/{groupId}/unset-admin")
    @Operation(summary = "取消管理员", description = "群主取消群管理员")
    public ResponseEntity<ApiResponse<String>> unsetGroupAdmin(
            @Parameter(description = "群组ID", required = true)
            @PathVariable Long groupId,
            @Parameter(description = "用户ID", required = true)
            @RequestParam Long userId,
            HttpServletRequest httpRequest) {
        try {
            Long ownerId = getUserIdFromToken(httpRequest);
            groupService.unsetGroupAdmin(ownerId, groupId, userId);
            return ResponseEntity.ok(ApiResponse.success("管理员取消成功"));
        } catch (Exception e) {
            log.error("取消管理员失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * 踢出群成员
     */
    @PostMapping("/{groupId}/kick/{memberId}")
    @Operation(summary = "踢出群成员", description = "管理员或群主踢出群成员")
    public ResponseEntity<ApiResponse<String>> kickGroupMember(
            @Parameter(description = "群组ID", required = true)
            @PathVariable Long groupId,
            @Parameter(description = "成员ID", required = true)
            @PathVariable Long memberId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            groupService.kickGroupMember(userId, groupId, memberId);
            return ResponseEntity.ok(ApiResponse.success("成员已踢出"));
        } catch (Exception e) {
            log.error("踢出群成员失败", e);
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