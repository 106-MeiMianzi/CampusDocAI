package com.campusdoc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "document")
public class DocumentProperties {

    private int maxFileSizeMb = 20;
    private int maxFilesPerRequest = 10;
    private int chunkSize = 600;
    private int chunkOverlap = 100;
    private int chunkStep = 500;
    /** 文档 iframe 预览短效 token 有效期（分钟） */
    private int previewTokenTtlMinutes = 15;
}
