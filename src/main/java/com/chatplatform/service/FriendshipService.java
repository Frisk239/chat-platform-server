package com.chatplatform.service;

import com.chatplatform.dto.request.FriendActionRequest;
import com.chatplatform.dto.request.FriendRequest;
import com.chatplatform.dto.response.FriendResponse;
import com.chatplatform.entity.Friendship;
import com.chatplatform.entity.User;
import com.chatplatform.repository.FriendshipRepository;
import com.chatplatform.repository.UserRepository;
import com.chatplatform.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 好友关系服务类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final WebSocketSessionManager sessionManager;

    /**
     * 发送好友申请
     */
    @Transactional
    public FriendResponse sendFriendRequest(Long userId, FriendRequest request) {
        log.info("发送好友申请: 用户={}, 目标用户={}", userId, request.getFriendId());

        // 验证用户存在
        User targetUser = userRepository.findById(request.getFriendId())
                .orElseThrow(() -> new RuntimeException("目标用户不存在"));

        // 不能添加自己为好友
        if (userId.equals(request.getFriendId())) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查是否已经是好友
        if (areFriends(userId, request.getFriendId())) {
            throw new RuntimeException("已经是好友关系");
        }

        // 检查是否已有待处理的申请
        Optional<Friendship> existingRequest = friendshipRepository
                .findByUserIdAndFriendIdAndStatus(userId, request.getFriendId(), 0);
        if (existingRequest.isPresent()) {
            throw new RuntimeException("已存在待处理的好友申请");
        }

        // 创建好友申请
        Friendship friendship = Friendship.builder()
                .userId(userId)
                .friendId(request.getFriendId())
                .requestMessage(request.getMessage())
                .remark(request.getRemark())
                .status(0) // 待确认
                .createdAt(LocalDateTime.now())
                .build();

        friendship = friendshipRepository.save(friendship);

        // 通知目标用户有新的好友申请
        notifyFriendRequest(targetUser.getId(), friendship);

        // 转换为响应DTO
        return FriendResponse.fromEntity(friendship, targetUser);
    }

    /**
     * 处理好友申请
     */
    @Transactional
    public FriendResponse handleFriendRequest(Long userId, FriendActionRequest request) {
        log.info("处理好友申请: 用户={}, 好友关系ID={}, 操作={}", userId, request.getFriendshipId(), request.getAction());

        Friendship friendship = friendshipRepository.findById(request.getFriendshipId())
                .orElseThrow(() -> new RuntimeException("好友申请不存在"));

        // 验证操作权限（只有接收者可以处理申请）
        if (!userId.equals(friendship.getFriendId())) {
            throw new RuntimeException("无权限处理此好友申请");
        }

        // 验证申请状态
        if (friendship.getStatus() != 0) {
            throw new RuntimeException("好友申请已被处理");
        }

        String action = request.getAction().toLowerCase();
        User friendUser = userRepository.findById(friendship.getUserId())
                .orElseThrow(() -> new RuntimeException("申请者用户不存在"));

        if ("accept".equals(action)) {
            // 接受好友申请
            friendship.setStatus(1); // 已确认
            friendship.setRemark(request.getRemark());
            friendship = friendshipRepository.save(friendship);

            // 创建双向好友关系
            createMutualFriendship(friendship.getUserId(), friendship.getFriendId());

            // 通知申请者申请被接受
            notifyFriendRequestAccepted(friendship.getUserId(), friendship);

        } else if ("reject".equals(action)) {
            // 拒绝好友申请
            friendship.setStatus(2); // 已拒绝
            friendship = friendshipRepository.save(friendship);

            // 通知申请者申请被拒绝
            notifyFriendRequestRejected(friendship.getUserId(), friendship);

        } else {
            throw new RuntimeException("无效的操作类型: " + action);
        }

        return FriendResponse.fromEntity(friendship, friendUser);
    }

    /**
     * 获取好友列表
     */
    @Transactional(readOnly = true)
    public List<FriendResponse> getFriendList(Long userId) {
        log.debug("获取好友列表: 用户={}", userId);

        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, 1);

        return friendships.stream()
                .map(friendship -> {
                    Optional<User> friendUser = userRepository.findById(friendship.getFriendId());
                    return friendUser.map(user -> FriendResponse.fromEntity(friendship, user))
                            .orElse(null);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 获取好友列表（分页）
     */
    @Transactional(readOnly = true)
    public Page<FriendResponse> getFriendList(Long userId, int page, int size) {
        log.debug("获取好友列表（分页）: 用户={}, page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Friendship> friendshipPage = friendshipRepository.findByUserIdAndStatus(userId, 1, pageable);

        List<Long> friendIds = friendshipPage.getContent().stream()
                .map(Friendship::getFriendId)
                .collect(Collectors.toList());

        List<User> friendUsers = userRepository.findByUserIds(friendIds);

        return friendshipPage.map(friendship -> {
            User friendUser = friendUsers.stream()
                    .filter(user -> user.getId().equals(friendship.getFriendId()))
                    .findFirst()
                    .orElse(null);
            return FriendResponse.fromEntity(friendship, friendUser);
        });
    }

    /**
     * 获取收到的好友申请
     */
    @Transactional(readOnly = true)
    public List<FriendResponse> getFriendRequests(Long userId) {
        log.debug("获取好友申请列表: 用户={}", userId);

        List<Friendship> requests = friendshipRepository.findByFriendIdAndStatus(userId, 0);

        return requests.stream()
                .map(request -> {
                    Optional<User> requesterUser = userRepository.findById(request.getUserId());
                    return requesterUser.map(user -> FriendResponse.fromEntity(request, user))
                            .orElse(null);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 删除好友
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        log.info("删除好友: 用户={}, 好友={}", userId, friendId);

        // 验证好友关系
        if (!areFriends(userId, friendId)) {
            throw new RuntimeException("不是好友关系");
        }

        // 删除双向好友关系
        friendshipRepository.deleteByUserIdAndFriendId(userId, friendId);
        friendshipRepository.deleteByUserIdAndFriendId(friendId, userId);

        // 通知对方好友被删除
        notifyFriendDeleted(friendId, userId);
    }

    /**
     * 更新好友备注
     */
    @Transactional
    public FriendResponse updateFriendRemark(Long userId, Long friendId, String remark) {
        log.info("更新好友备注: 用户={}, 好友={}, 备注={}", userId, friendId, remark);

        Friendship friendship = friendshipRepository.findByUserIdAndFriendIdAndStatus(userId, friendId, 1)
                .orElseThrow(() -> new RuntimeException("好友关系不存在"));

        friendship.setRemark(remark);
        friendship = friendshipRepository.save(friendship);

        User friendUser = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("好友用户不存在"));

        return FriendResponse.fromEntity(friendship, friendUser);
    }

    /**
     * 搜索好友
     */
    @Transactional(readOnly = true)
    public List<FriendResponse> searchFriends(Long userId, String keyword) {
        log.debug("搜索好友: 用户={}, 关键词={}", userId, keyword);

        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, 1);

        return friendships.stream()
                .filter(friendship -> {
                    Optional<User> friendUser = userRepository.findById(friendship.getFriendId());
                    return friendUser.filter(user ->
                        user.getNickname().toLowerCase().contains(keyword.toLowerCase()) ||
                        user.getQqNumber().contains(keyword) ||
                        (friendship.getRemark() != null && friendship.getRemark().toLowerCase().contains(keyword.toLowerCase()))
                    ).isPresent();
                })
                .map(friendship -> {
                    User friendUser = userRepository.findById(friendship.getFriendId()).get();
                    return FriendResponse.fromEntity(friendship, friendUser);
                })
                .collect(Collectors.toList());
    }

    /**
     * 检查是否为好友
     */
    @Transactional(readOnly = true)
    public boolean areFriends(Long userId1, Long userId2) {
        return friendshipRepository.areFriends(userId1, userId2);
    }

    /**
     * 获取好友统计
     */
    @Transactional(readOnly = true)
    public FriendshipStats getFriendStats(Long userId) {
        long totalFriends = friendshipRepository.countFriendsByUserId(userId);
        long pendingRequests = friendshipRepository.countFriendRequestsByUserId(userId);

        return FriendshipStats.builder()
                .totalFriends(totalFriends)
                .pendingRequests(pendingRequests)
                .build();
    }

    /**
     * 获取可能认识的人（好友的好友）
     * 暂时注释掉以解决启动问题，待后续实现
     */
    @Transactional(readOnly = true)
    public List<User> getRecommendedFriends(Long userId, int limit) {
        // 暂时返回空列表，待实现复杂查询后恢复
        // List<Long> friendOfFriends = friendshipRepository.findFriendsOfFriends(userId);
        // return friendOfFriends.stream()
        //         .distinct()
        //         .limit(limit)
        //         .map(userRepository::findById)
        //         .filter(Optional::isPresent)
        //         .map(Optional::get)
        //         .collect(Collectors.toList());

        return new ArrayList<>();
    }

    /**
     * 创建双向好友关系
     */
    private void createMutualFriendship(Long userId1, Long userId2) {
        // 检查是否已存在反向关系
        Optional<Friendship> reverseFriendship = friendshipRepository
                .findByUserIdAndFriendId(userId2, userId1);

        if (reverseFriendship.isEmpty()) {
            // 创建反向关系
            Friendship mutualFriendship = Friendship.builder()
                    .userId(userId2)
                    .friendId(userId1)
                    .status(1) // 已确认
                    .requestMessage("自动确认好友关系")
                    .createdAt(LocalDateTime.now())
                    .build();
            friendshipRepository.save(mutualFriendship);
        } else {
            // 更新反向关系状态
            Friendship reverse = reverseFriendship.get();
            reverse.setStatus(1);
            reverse.setCreatedAt(LocalDateTime.now());
            friendshipRepository.save(reverse);
        }
    }

    /**
     * 通知新好友申请
     */
    private void notifyFriendRequest(Long userId, Friendship friendship) {
        Map<String, Object> notification = Map.of(
                "type", "friend_request",
                "friendshipId", friendship.getId(),
                "requesterId", friendship.getUserId(),
                "requesterNickname", getUserNickname(friendship.getUserId()),
                "requesterQqNumber", getUserQqNumber(friendship.getUserId()),
                "message", friendship.getRequestMessage(),
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(userId, notification);
    }

    /**
     * 通知好友申请被接受
     */
    private void notifyFriendRequestAccepted(Long userId, Friendship friendship) {
        Map<String, Object> notification = Map.of(
                "type", "friend_request_accepted",
                "friendId", friendship.getFriendId(),
                "friendNickname", getUserNickname(friendship.getFriendId()),
                "friendQqNumber", getUserQqNumber(friendship.getFriendId()),
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(userId, notification);
    }

    /**
     * 通知好友申请被拒绝
     */
    private void notifyFriendRequestRejected(Long userId, Friendship friendship) {
        Map<String, Object> notification = Map.of(
                "type", "friend_request_rejected",
                "friendId", friendship.getFriendId(),
                "friendNickname", getUserNickname(friendship.getFriendId()),
                "friendQqNumber", getUserQqNumber(friendship.getFriendId()),
                "message", friendship.getRequestMessage(),
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(userId, notification);
    }

    /**
     * 通知好友被删除
     */
    private void notifyFriendDeleted(Long friendId, Long userId) {
        Map<String, Object> notification = Map.of(
                "type", "friend_deleted",
                "friendId", userId,
                "friendNickname", getUserNickname(userId),
                "friendQqNumber", getUserQqNumber(userId),
                "timestamp", System.currentTimeMillis()
        );

        sessionManager.sendToUser(friendId, notification);
    }

    /**
     * 获取用户昵称
     */
    private String getUserNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .orElse("未知用户");
    }

    /**
     * 获取用户QQ号
     */
    private String getUserQqNumber(Long userId) {
        return userRepository.findById(userId)
                .map(User::getQqNumber)
                .orElse("0");
    }

    /**
     * 好友统计信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FriendshipStats {
        private Long totalFriends;
        private Long pendingRequests;
    }
}