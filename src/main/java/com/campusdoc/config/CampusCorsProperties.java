package com.campusdoc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "campus.cors")
public class CampusCorsProperties {

    /**
     * 开发期可设为 {@code *}；生产请改为具体前端域名（逗号分隔多个）。
     */
    private String allowedOrigins = "*";
}
