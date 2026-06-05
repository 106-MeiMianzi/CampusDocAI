package com.campusdoc.security;

import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.config.CampusDebugProperties;
import com.campusdoc.user.service.AuthTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenService authTokenService;
    private final CampusDebugProperties campusDebugProperties;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   AuthTokenService authTokenService,
                                   CampusDebugProperties campusDebugProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authTokenService = authTokenService;
        this.campusDebugProperties = campusDebugProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = resolveBearerToken(authHeader);

        if (token != null) {
            try {
                Long userId = jwtTokenProvider.getUserId(token);
                String stored = authTokenService.getStoredToken(userId);
                boolean active = stored != null && stored.equals(token);
                if (!active) {
                    logAuth(request, authHeader, token, userId, stored, false,
                            "Redis 中无有效 token 或与请求 token 不一致（可能已登出、重复登录覆盖了旧 token）");
                    throw new BusinessException(ErrorCode.UNAUTHORIZED);
                }
                String username = jwtTokenProvider.parseClaims(token).get("username", String.class);
                AuthUser authUser = new AuthUser(userId, username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(authUser, null, java.util.List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logAuth(request, authHeader, token, userId, stored, true, "鉴权成功");
            } catch (JwtException ex) {
                SecurityContextHolder.clearContext();
                logAuth(request, authHeader, token, null, null, false,
                        "JWT 解析/校验失败: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            } catch (BusinessException ex) {
                SecurityContextHolder.clearContext();
                if (!campusDebugProperties.isRequestLog()) {
                    logAuth(request, authHeader, token, null, null, false, ex.getErrorCode().getMessage());
                }
            }
        } else if (campusDebugProperties.isRequestLog()) {
            log.info("========== JWT 鉴权 ==========\nuri={}\nauthorizationHeader(raw)={}\nresolvedToken=<null>\nresult=未携带 token，需登录接口将由 Security 返回 401",
                    request.getRequestURI(), authHeader == null ? "<null>" : authHeader);
        }
        chain.doFilter(request, response);
    }

    private void logAuth(HttpServletRequest request, String authHeader, String requestToken,
                         Long userId, String redisToken, boolean success, String reason) {
        if (!campusDebugProperties.isRequestLog()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("uri=").append(request.getRequestURI()).append('\n');
        sb.append("authorizationHeader(raw)=").append(authHeader == null ? "<null>" : authHeader).append('\n');
        sb.append("requestToken(resolved)=").append(requestToken).append('\n');
        if (userId != null) {
            sb.append("jwtUserId=").append(userId).append('\n');
            sb.append("redisKey=").append(authTokenService.redisKey(userId)).append('\n');
            sb.append("redisToken(stored)=").append(redisToken == null ? "<null>" : redisToken).append('\n');
            sb.append("tokenMatch=").append(requestToken != null && requestToken.equals(redisToken)).append('\n');
            if (requestToken != null && redisToken != null && !requestToken.equals(redisToken)) {
                sb.append("requestTokenLength=").append(requestToken.length()).append('\n');
                sb.append("redisTokenLength=").append(redisToken.length()).append('\n');
            }
        }
        sb.append("result=").append(success ? "SUCCESS" : "FAIL").append('\n');
        sb.append("reason=").append(reason);
        log.info("========== JWT 鉴权 ==========\n{}", sb);
    }

    /**
     * 支持 {@code Bearer <token>} 与直接传 token（Apifox 等工具常把变量填在 Authorization 里）。
     */
    static String resolveBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String trimmed = authorizationHeader.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = trimmed.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return trimmed;
    }
}
