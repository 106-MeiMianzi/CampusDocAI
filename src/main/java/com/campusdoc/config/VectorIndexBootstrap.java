package com.campusdoc.config;

import com.campusdoc.ai.service.VectorReindexService;
import com.campusdoc.ai.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class VectorIndexBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorIndexBootstrap.class);
    private static final String SCHEMA_VERSION_KEY = "campus:vector:schema-version";

    private final VectorStoreService vectorStoreService;
    private final VectorReindexService vectorReindexService;
    private final StringRedisTemplate stringRedisTemplate;
    private final int schemaVersion;

    public VectorIndexBootstrap(VectorStoreService vectorStoreService,
                                VectorReindexService vectorReindexService,
                                StringRedisTemplate stringRedisTemplate,
                                @Value("${redis.vector-index-schema-version:2}") int schemaVersion) {
        this.vectorStoreService = vectorStoreService;
        this.vectorReindexService = vectorReindexService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.schemaVersion = schemaVersion;
    }

    @Override
    public void run(ApplicationArguments args) {
        String expected = String.valueOf(schemaVersion);
        String stored = stringRedisTemplate.opsForValue().get(SCHEMA_VERSION_KEY);
        if (expected.equals(stored) && vectorStoreService.indexHasMetadataFields()) {
            return;
        }
        log.warn("Redis vector index schema outdated (stored={}, expected={}), rebuilding index and re-embedding chunks",
                stored, expected);
        // 须在重建 RedisEmbeddingStore 之前删除旧索引：索引仅在 store 构造时创建，否则会出现有 embedding 键但无 FT 索引
        vectorStoreService.dropVectorIndex(true);
        vectorStoreService.rebuildEmbeddingStore();
        int chunks = vectorReindexService.reindexFromChunkTable();
        stringRedisTemplate.opsForValue().set(SCHEMA_VERSION_KEY, expected);
        log.info("Redis vector index rebuilt, {} chunks indexed", chunks);
    }
}
