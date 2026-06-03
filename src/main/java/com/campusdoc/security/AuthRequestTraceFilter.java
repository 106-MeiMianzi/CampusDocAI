package com.campusdoc.security;

import com.campusdoc.config.CampusDebugProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;

/**
 * 联调排错：打印每个请求的完整入参与鉴权结果（由 {@code campus.debug.request-log} 开关控制）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuthRequestTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRequestTraceFilter.class);

    private final CampusDebugProperties campusDebugProperties;

    public AuthRequestTraceFilter(CampusDebugProperties campusDebugProperties) {
        this.campusDebugProperties = campusDebugProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !campusDebugProperties.isRequestLog();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        log.info("========== HTTP 请求开始 ==========\n{}", buildRequestLog(request));
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("========== HTTP 请求结束 ==========\n{}",
                    buildResponseLog(request, response, System.currentTimeMillis() - start));
        }
    }

    private static String buildRequestLog(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("method=").append(request.getMethod()).append('\n');
        sb.append("uri=").append(request.getRequestURI()).append('\n');
        if (request.getQueryString() != null) {
            sb.append("query=").append(request.getQueryString()).append('\n');
        }
        sb.append("remote=").append(request.getRemoteAddr()).append('\n');
        sb.append("headers=\n");
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                sb.append("  ").append(name).append(": ");
                appendHeaderValues(sb, request, name);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String buildResponseLog(HttpServletRequest request, HttpServletResponse response, long ms) {
        StringBuilder sb = new StringBuilder();
        sb.append("method=").append(request.getMethod()).append('\n');
        sb.append("uri=").append(request.getRequestURI()).append('\n');
        sb.append("status=").append(response.getStatus()).append('\n');
        sb.append("elapsedMs=").append(ms).append('\n');
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            sb.append("securityContext=无 Authentication\n");
        } else {
            sb.append("securityContext.principal=").append(auth.getPrincipal()).append('\n');
            sb.append("securityContext.authenticated=").append(auth.isAuthenticated()).append('\n');
        }
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        sb.append("authorizationHeader(raw)=").append(authHeader == null ? "<null>" : authHeader).append('\n');
        String resolved = JwtAuthenticationFilter.resolveBearerToken(authHeader);
        sb.append("authorizationToken(resolved)=").append(resolved == null ? "<null>" : resolved).append('\n');
        return sb.toString();
    }

    private static void appendHeaderValues(StringBuilder sb, HttpServletRequest request, String name) {
        Enumeration<String> values = request.getHeaders(name);
        if (values == null) {
            sb.append("<null>");
            return;
        }
        boolean first = true;
        while (values.hasMoreElements()) {
            if (!first) {
                sb.append(" | ");
            }
            sb.append(values.nextElement());
            first = false;
        }
    }
}
