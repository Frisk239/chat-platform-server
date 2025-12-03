package com.chatplatform.service;

import com.chatplatform.dto.request.UserRegisterRequest;
import com.chatplatform.dto.request.UserLoginRequest;
import com.chatplatform.dto.response.UserResponse;
import com.chatplatform.entity.User;
import com.chatplatform.repository.UserRepository;
import com.chatplatform.util.JwtTokenProvider;
import com.chatplatform.util.QQNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户服务类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QQNumberGenerator qqNumberGenerator;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 注册后的用户信息
     */
    @Transactional
    public UserResponse register(UserRegisterRequest registerRequest) {
        log.info("开始用户注册: username={}", registerRequest.getUsername());

        // 验证确认密码
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在（如果提供了邮箱）
        if (StringUtils.hasText(registerRequest.getEmail()) &&
            userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已被使用");
        }

        // 检查手机号是否已存在（如果提供了手机号）
        if (StringUtils.hasText(registerRequest.getPhone()) &&
            userRepository.existsByPhone(registerRequest.getPhone())) {
            throw new RuntimeException("手机号已被使用");
        }

        try {
            // 生成QQ号
            String qqNumber = qqNumberGenerator.generateQQNumber();

            // 创建用户
            User user = User.builder()
                    .qqNumber(qqNumber)
                    .username(registerRequest.getUsername())
                    .nickname(registerRequest.getNickname())
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .email(registerRequest.getEmail())
                    .phone(registerRequest.getPhone())
                    .gender(registerRequest.getGender())
                    .signature(registerRequest.getSignature())
                    .status(0) // 注册时默认离线状态
                    .createdAt(LocalDateTime.now())
                    .build();

            // 处理生日
            if (StringUtils.hasText(registerRequest.getBirthday())) {
                try {
                    user.setBirthday(LocalDate.parse(registerRequest.getBirthday()));
                } catch (Exception e) {
                    log.warn("生日格式解析失败: {}", registerRequest.getBirthday());
                    // 生日格式错误时不设置，继续注册流程
                }
            }

            // 保存用户
            user = userRepository.save(user);

            log.info("用户注册成功: userId={}, qqNumber={}, username={}",
                    user.getId(), user.getQqNumber(), user.getUsername());

            return UserResponse.createLoginUser(user);

        } catch (Exception e) {
            log.error("用户注册失败: username={}", registerRequest.getUsername(), e);
            throw new RuntimeException("注册失败: " + e.getMessage(), e);
        }
    }

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录结果（包含用户信息和Token）
     */
    @Transactional
    public LoginResult login(UserLoginRequest loginRequest) {
        log.info("用户登录: {}", loginRequest.getUsernameOrQqNumber());

        // 查找用户（支持用户名或QQ号登录）
        User user = findUserByUsernameOrQqNumber(loginRequest.getUsernameOrQqNumber());

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        try {
            // 更新最后登录时间
            user.updateLastLoginTime();
            userRepository.save(user);

            // 生成Token
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

            // 构建Token信息
            JwtTokenProvider.TokenInfo tokenInfo = jwtTokenProvider.getTokenInfo(accessToken, refreshToken);

            log.info("用户登录成功: userId={}, qqNumber={}", user.getId(), user.getQqNumber());

            return LoginResult.builder()
                    .user(UserResponse.createLoginUser(user))
                    .tokenInfo(tokenInfo)
                    .build();

        } catch (Exception e) {
            log.error("用户登录失败: username={}", loginRequest.getUsernameOrQqNumber(), e);
            throw new RuntimeException("登录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新Token
     * @return 新的Token信息
     */
    public JwtTokenProvider.TokenInfo refreshToken(String refreshToken) {
        log.info("刷新Token");

        try {
            // 验证刷新Token
            if (!jwtTokenProvider.validateToken(refreshToken) ||
                !jwtTokenProvider.isRefreshToken(refreshToken)) {
                throw new RuntimeException("无效的刷新Token");
            }

            // 获取用户信息
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                throw new RuntimeException("用户不存在");
            }

            User user = userOptional.get();

            // 生成新的Token
            String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

            JwtTokenProvider.TokenInfo tokenInfo = jwtTokenProvider.getTokenInfo(newAccessToken, newRefreshToken);

            log.info("Token刷新成功: userId={}", userId);

            return tokenInfo;

        } catch (Exception e) {
            log.error("刷新Token失败", e);
            throw new RuntimeException("刷新Token失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据用户名或QQ号查找用户
     */
    private User findUserByUsernameOrQqNumber(String usernameOrQqNumber) {
        // 先尝试按用户名查找
        Optional<User> userOptional = userRepository.findByUsername(usernameOrQqNumber);
        if (userOptional.isPresent()) {
            return userOptional.get();
        }

        // 再尝试按QQ号查找
        userOptional = userRepository.findByQqNumber(usernameOrQqNumber);
        if (userOptional.isPresent()) {
            return userOptional.get();
        }

        return null;
    }

    /**
     * 根据ID获取用户信息
     */
    public UserResponse getUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.map(UserResponse::createFullUser).orElse(null);
    }

    /**
     * 根据QQ号获取用户信息
     */
    public UserResponse getUserByQqNumber(String qqNumber) {
        Optional<User> userOptional = userRepository.findByQqNumber(qqNumber);
        return userOptional.map(UserResponse::createFullUser).orElse(null);
    }

    /**
     * 搜索用户
     */
    public Page<UserResponse> searchUsers(String keyword, Long currentUserId, Pageable pageable) {
        Page<User> userPage = userRepository.searchUsers(keyword, currentUserId, pageable);
        return userPage.map(UserResponse::createSimpleUser);
    }

    /**
     * 获取用户列表（根据ID列表）
     */
    public List<UserResponse> getUsersByIds(List<Long> userIds) {
        List<User> users = userRepository.findByUserIds(userIds);
        return users.stream()
                .map(UserResponse::createSimpleUser)
                .collect(Collectors.toList());
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest updateRequest) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOptional.get();

        // 更新昵称
        if (StringUtils.hasText(updateRequest.getNickname())) {
            user.setNickname(updateRequest.getNickname());
        }

        // 更新头像
        if (StringUtils.hasText(updateRequest.getAvatarUrl())) {
            user.setAvatarUrl(updateRequest.getAvatarUrl());
        }

        // 更新个性签名
        if (StringUtils.hasText(updateRequest.getSignature())) {
            user.setSignature(updateRequest.getSignature());
        }

        // 更新性别
        if (updateRequest.getGender() != null) {
            user.setGender(updateRequest.getGender());
        }

        // 更新生日
        if (updateRequest.getBirthday() != null) {
            user.setBirthday(updateRequest.getBirthday());
        }

        // 更新邮箱
        if (StringUtils.hasText(updateRequest.getEmail()) &&
            !updateRequest.getEmail().equals(user.getEmail())) {
            // 检查邮箱是否已被其他用户使用
            if (userRepository.existsByEmail(updateRequest.getEmail())) {
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(updateRequest.getEmail());
        }

        // 更新手机号
        if (StringUtils.hasText(updateRequest.getPhone()) &&
            !updateRequest.getPhone().equals(user.getPhone())) {
            // 检查手机号是否已被其他用户使用
            if (userRepository.existsByPhone(updateRequest.getPhone())) {
                throw new RuntimeException("手机号已被使用");
            }
            user.setPhone(updateRequest.getPhone());
        }

        user = userRepository.save(user);

        log.info("用户信息更新成功: userId={}", userId);

        return UserResponse.createFullUser(user);
    }

    /**
     * 更新用户在线状态
     */
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOptional.get();
        user.setStatus(status);
        userRepository.save(user);

        log.debug("用户状态更新: userId={}, status={}", userId, status);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOptional.get();

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("用户密码修改成功: userId={}", userId);
    }

    /**
     * 用户信息更新请求DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserUpdateRequest {
        private String nickname;
        private String avatarUrl;
        private String signature;
        private Integer gender;
        private LocalDate birthday;
        private String email;
        private String phone;
    }

    /**
     * 登录结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LoginResult {
        private UserResponse user;
        private JwtTokenProvider.TokenInfo tokenInfo;
    }

    // ==================== UserDetailsService接口实现 ====================

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrQqNumber) throws UsernameNotFoundException {
        log.debug("加载用户详情: {}", usernameOrQqNumber);

        // 查找用户（支持用户名或QQ号登录）
        User user = findUserByUsernameOrQqNumber(usernameOrQqNumber);

        if (user == null) {
            log.warn("用户不存在: {}", usernameOrQqNumber);
            throw new UsernameNotFoundException("用户不存在: " + usernameOrQqNumber);
        }

        // 创建用户权限（暂时都给予普通用户权限）
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");

        // 构建UserDetails对象
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername()) // 使用用户名作为认证标识
                .password(user.getPassword())
                .authorities(Collections.singletonList(authority))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * 根据用户ID查找用户详情
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("根据ID加载用户详情: {}", userId);

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("用户不存在: userId={}", userId);
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }

        User user = userOptional.get();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(authority))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}