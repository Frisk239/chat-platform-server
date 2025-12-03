package com.chatplatform.security;

import com.chatplatform.entity.User;
import com.chatplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

/**
 * 自定义用户详情服务
 * 用于Spring Security的用户认证
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

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