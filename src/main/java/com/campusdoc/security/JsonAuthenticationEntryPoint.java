package com.campusdoc.security;

import com.campusdoc.common.ApiResponse;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.config.CampusDebugProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;
    private final CampusDebugProperties campusDebugProperties;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper,
                                          CampusDebugProperties campusDebugProperties) {
        this.objectMapper = objectMapper;
        this.campusDebugProperties = campusDebugProperties;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws java.io.IOException {
        if (campusDebugProperties.isRequestLog()) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String resolved = JwtAuthenticationFilter.resolveBearerToken(authHeader);
            log.warn("========== 未认证拦截 401 ==========\nuri={}\nauthorizationHeader(raw)={}\nresolvedToken={}\nexception={}",
                    request.getRequestURI(),
                    authHeader == null ? "<null>" : authHeader,
                    resolved == null ? "<null>" : resolved,
                    authException.getMessage());
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(ErrorCode.UNAUTHORIZED));
    }
}
