package com.campusdoc.security;

import com.campusdoc.config.CampusDebugProperties;
import com.campusdoc.document.service.DocumentPreviewTokenService;
import com.campusdoc.user.service.AuthTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 允许 iframe 通过 URL {@code ?token=} 访问文档文件（短效预览 token）。
 * 仅注册在 Spring Security 过滤链中，勿加 {@code @Component}（避免 Servlet 链重复注册导致鉴权被跳过）。
 */
public class DocumentPreviewTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DocumentPreviewTokenFilter.class);

    private static final Pattern FILE_PATH = Pattern.compile("/api/document/(\\d+)/file/?");

    private final DocumentPreviewTokenService previewTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenService authTokenService;
    private final CampusDebugProperties campusDebugProperties;

    public DocumentPreviewTokenFilter(DocumentPreviewTokenService previewTokenService,
                                      JwtTokenProvider jwtTokenProvider,
                                      AuthTokenService authTokenService,
                                      CampusDebugProperties campusDebugProperties) {
        this.previewTokenService = previewTokenService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authTokenService = authTokenService;
        this.campusDebugProperties = campusDebugProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        Long documentId = extractDocumentId(request.getRequestURI());
        if (documentId == null) {
            chain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof AuthUser) {
            chain.doFilter(request, response);
            return;
        }

        String token = request.getParameter("token");
        if (token == null || token.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        if (authenticatePreviewToken(token, documentId, request.getRequestURI())
                || authenticateJwtQueryToken(token, request.getRequestURI())) {
            // authenticated
        } else if (campusDebugProperties.isRequestLog()) {
            log.info("========== 预览 Token 鉴权 ==========\nuri={}\ndocumentId={}\nresult=FAIL\nreason=token 无效、已过期或与文档不匹配",
                    request.getRequestURI(), documentId);
        }

        chain.doFilter(request, response);
    }

    private boolean authenticatePreviewToken(String token, Long documentId, String uri) {
        var validated = previewTokenService.validate(token, documentId);
        if (validated.isEmpty()) {
            return false;
        }
        var payload = validated.get();
        setAuthentication(new AuthUser(payload.userId(), "preview"));
        if (campusDebugProperties.isRequestLog()) {
            log.info("========== 预览 Token 鉴权 ==========\nuri={}\ndocumentId={}\nuserId={}\nresult=SUCCESS\ntype=preview",
                    uri, documentId, payload.userId());
        }
        return true;
    }

    /** 兼容前端误将登录 JWT 放入 ?token= 的场景（iframe 无法携带 Authorization 头） */
    private boolean authenticateJwtQueryToken(String token, String uri) {
        if (!token.regionMatches(true, 0, "eyJ", 0, 3)) {
            return false;
        }
        try {
            Long userId = jwtTokenProvider.getUserId(token);
            String stored = authTokenService.getStoredToken(userId);
            if (stored == null || !stored.equals(token)) {
                if (campusDebugProperties.isRequestLog()) {
                    log.info("========== 预览 Token 鉴权 ==========\nuri={}\nresult=FAIL\ntype=jwt-fallback\nreason=Redis 中无有效 token",
                            uri);
                }
                return false;
            }
            String username = jwtTokenProvider.parseClaims(token).get("username", String.class);
            setAuthentication(new AuthUser(userId, username));
            if (campusDebugProperties.isRequestLog()) {
                log.info("========== 预览 Token 鉴权 ==========\nuri={}\nuserId={}\nresult=SUCCESS\ntype=jwt-fallback",
                        uri, userId);
            }
            return true;
        } catch (JwtException | NumberFormatException ex) {
            if (campusDebugProperties.isRequestLog()) {
                log.info("========== 预览 Token 鉴权 ==========\nuri={}\nresult=FAIL\ntype=jwt-fallback\nreason={}",
                        uri, ex.getMessage());
            }
            return false;
        }
    }

    private static void setAuthentication(AuthUser authUser) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authUser, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Long extractDocumentId(String uri) {
        if (uri == null) {
            return null;
        }
        Matcher matcher = FILE_PATH.matcher(uri);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
