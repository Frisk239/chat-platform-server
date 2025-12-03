package com.chatplatform.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT Token 提供者
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 生成访问Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT Token
     */
    public String generateAccessToken(Long userId, String username) {
        return generateToken(userId, username, "access", expiration);
    }

    /**
     * 生成刷新Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT Refresh Token
     */
    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, "refresh", refreshExpiration);
    }

    /**
     * 生成Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param tokenType Token类型
     * @param expiration 过期时间
     * @return JWT Token
     */
    private String generateToken(Long userId, String username, String tokenType, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withSubject(String.valueOf(userId))
                    .withClaim("username", username)
                    .withClaim("tokenType", tokenType)
                    .withIssuedAt(now)
                    .withExpiresAt(expiryDate)
                    .sign(algorithm);
        } catch (Exception e) {
            log.error("生成JWT Token失败: userId={}, username={}, tokenType={}", userId, username, tokenType, e);
            throw new RuntimeException("生成JWT Token失败", e);
        }
    }

    /**
     * 验证Token
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("JWT Token验证失败: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT Token验证异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证Token并获取解码后的JWT
     *
     * @param token JWT Token
     * @return 解码后的JWT
     * @throws JWTVerificationException 验证失败异常
     */
    public DecodedJWT validateAndDecodeToken(String token) throws JWTVerificationException {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.warn("JWT Token验证失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("JWT Token验证异常: {}", e.getMessage(), e);
            throw new JWTVerificationException("Token验证异常");
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = validateAndDecodeToken(token);
            return Long.valueOf(jwt.getSubject());
        } catch (Exception e) {
            log.error("从Token中获取用户ID失败: {}", e.getMessage(), e);
            throw new RuntimeException("无效的Token", e);
        }
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            DecodedJWT jwt = validateAndDecodeToken(token);
            return jwt.getClaim("username").asString();
        } catch (Exception e) {
            log.error("从Token中获取用户名失败: {}", e.getMessage(), e);
            throw new RuntimeException("无效的Token", e);
        }
    }

    /**
     * 从Token中获取Token类型
     *
     * @param token JWT Token
     * @return Token类型
     */
    public String getTokenTypeFromToken(String token) {
        try {
            DecodedJWT jwt = validateAndDecodeToken(token);
            return jwt.getClaim("tokenType").asString();
        } catch (Exception e) {
            log.error("从Token中获取Token类型失败: {}", e.getMessage(), e);
            throw new RuntimeException("无效的Token", e);
        }
    }

    /**
     * 检查是否为访问Token
     *
     * @param token JWT Token
     * @return 是否为访问Token
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenTypeFromToken(token));
    }

    /**
     * 检查是否为刷新Token
     *
     * @param token JWT Token
     * @return 是否为刷新Token
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenTypeFromToken(token));
    }

    /**
     * 获取Token的过期时间
     *
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationFromToken(String token) {
        try {
            DecodedJWT jwt = validateAndDecodeToken(token);
            return jwt.getExpiresAt();
        } catch (Exception e) {
            log.error("从Token中获取过期时间失败: {}", e.getMessage(), e);
            throw new RuntimeException("无效的Token", e);
        }
    }

    /**
     * 检查Token是否过期
     *
     * @param token JWT Token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true; // 如果解析失败，认为Token无效
        }
    }

    /**
     * 检查Token是否即将过期（30分钟内）
     *
     * @param token JWT Token
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            Date thirtyMinutesFromNow = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
            return expiration.before(thirtyMinutesFromNow);
        } catch (Exception e) {
            return true; // 如果解析失败，认为Token即将过期
        }
    }

    /**
     * 刷新访问Token
     *
     * @param refreshToken 刷新Token
     * @return 新的访问Token
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            if (!validateToken(refreshToken) || !isRefreshToken(refreshToken)) {
                throw new RuntimeException("无效的刷新Token");
            }

            Long userId = getUserIdFromToken(refreshToken);
            String username = getUsernameFromToken(refreshToken);

            return generateAccessToken(userId, username);
        } catch (Exception e) {
            log.error("刷新访问Token失败: {}", e.getMessage(), e);
            throw new RuntimeException("刷新Token失败", e);
        }
    }

    /**
     * 解析Token（不验证）
     *
     * @param token JWT Token
     * @return 解码后的JWT
     */
    public DecodedJWT decodeToken(String token) {
        try {
            return JWT.decode(token);
        } catch (Exception e) {
            log.error("解析Token失败: {}", e.getMessage(), e);
            throw new RuntimeException("无效的Token格式", e);
        }
    }

    /**
     * 从请求头中提取Token
     *
     * @param authHeader Authorization header
     * @return JWT Token
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 生成Token信息响应
     *
     * @param accessToken 访问Token
     * @param refreshToken 刷新Token
     * @return Token信息
     */
    public TokenInfo getTokenInfo(String accessToken, String refreshToken) {
        Date accessExpiration = getExpirationFromToken(accessToken);
        Date refreshExpiration = getExpirationFromToken(refreshToken);

        return TokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(accessExpiration.getTime() - System.currentTimeMillis())
                .refreshTokenExpiresIn(refreshExpiration.getTime() - System.currentTimeMillis())
                .build();
    }

    /**
     * Token信息类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class TokenInfo {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long accessTokenExpiresIn;
        private Long refreshTokenExpiresIn;
    }
}