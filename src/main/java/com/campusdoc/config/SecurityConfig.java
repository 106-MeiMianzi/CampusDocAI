package com.campusdoc.config;

import com.campusdoc.document.service.DocumentPreviewTokenService;
import com.campusdoc.security.DocumentPreviewTokenFilter;
import com.campusdoc.security.JsonAuthenticationEntryPoint;
import com.campusdoc.security.JwtAuthenticationFilter;
import com.campusdoc.security.JwtTokenProvider;
import com.campusdoc.user.service.AuthTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final CampusCorsProperties campusCorsProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
                          CampusCorsProperties campusCorsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jsonAuthenticationEntryPoint = jsonAuthenticationEntryPoint;
        this.campusCorsProperties = campusCorsProperties;
    }

    @Bean
    public DocumentPreviewTokenFilter documentPreviewTokenFilter(DocumentPreviewTokenService previewTokenService,
                                                                 JwtTokenProvider jwtTokenProvider,
                                                                 AuthTokenService authTokenService,
                                                                 CampusDebugProperties campusDebugProperties) {
        return new DocumentPreviewTokenFilter(
                previewTokenService, jwtTokenProvider, authTokenService, campusDebugProperties);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DocumentPreviewTokenFilter documentPreviewTokenFilter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user/avatar/image/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jsonAuthenticationEntryPoint))
                // 二者均挂在 UsernamePasswordAuthenticationFilter 前；先注册 preview、再注册 JWT → 实际顺序 preview → JWT
                .addFilterBefore(documentPreviewTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        applyAllowedOrigins(config);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void applyAllowedOrigins(CorsConfiguration config) {
        String raw = campusCorsProperties.getAllowedOrigins();
        if (raw == null || raw.isBlank() || "*".equals(raw.trim())) {
            config.setAllowedOrigins(List.of("*"));
            return;
        }
        List<String> origins = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);
    }
}
