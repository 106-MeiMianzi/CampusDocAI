package com.campusdoc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "campus.debug")
public class CampusDebugProperties {

    /**
     * 为 true 时，每个 HTTP 请求在控制台打印完整请求头、鉴权解析与 Redis token 比对结果（仅用于联调排错）。
     */
    private boolean requestLog = true;
}
