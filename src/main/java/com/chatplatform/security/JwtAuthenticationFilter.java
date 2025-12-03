package com.chatplatform.security;

import com.chatplatform.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器 - 处理每个请求的JWT验证
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                  @Qualifier("customUserDetailsService") UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("JWT过滤器处理请求: {}", requestURI);

        try {
            // 从请求头中获取JWT
            String jwt = getJwtFromRequest(request);
            log.debug("从请求中提取的JWT: {}", jwt != null ? jwt.substring(0, Math.min(jwt.length(), 20)) + "..." : "null");

            if (StringUtils.hasText(jwt)) {
                if (jwtTokenProvider.validateToken(jwt)) {
                    // 获取用户名
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    log.debug("JWT验证成功，用户名: {}", username);

                    // 加载用户详情
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    log.debug("用户详情加载成功: {}", userDetails.getUsername());

                    // 创建认证令牌
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // 设置认证详情
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置到安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("用户认证成功: {}, 请求路径: {}", username, requestURI);
                } else {
                    log.warn("JWT验证失败，请求路径: {}", requestURI);
                }
            } else {
                log.debug("请求中没有JWT token，请求路径: {}", requestURI);
            }
        } catch (Exception ex) {
            log.error("JWT认证过程中发生异常，请求路径: {}, 错误: {}", requestURI, ex.getMessage(), ex);
            // 不抛出异常，让过滤器链继续执行
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取JWT
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}