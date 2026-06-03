package com.campusdoc.ai.service;

import com.campusdoc.config.AiProperties;
import com.campusdoc.document.entity.DocChunkEntity;
import com.campusdoc.config.RedisVectorConfig.RedisHostPort;
import com.campusdoc.user.entity.UserRole;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final AiProperties aiProperties;
    private final RedisHostPort redisHostPort;
    private final String indexName;

    private RedisEmbeddingStore embeddingStore;

    public VectorStoreService(AiProperties aiProperties,
                              RedisHostPort redisHostPort,
                              @Value("${redis.vector-index-name}") String indexName) {
        this.aiProperties = aiProperties;
        this.redisHostPort = redisHostPort;
        this.indexName = indexName;
    }

    private static final Set<String> VECTOR_METADATA_KEYS =
            Set.of("userId", "documentId", "chunkId", "fileName", "shared");

    @PostConstruct
    public void init() {
        rebuildEmbeddingStore();
    }

    public void rebuildEmbeddingStore() {
        embeddingStore = RedisEmbeddingStore.builder()
                .host(redisHostPort.host())
                .port(redisHostPort.port())
                .indexName(indexName)
                .dimension(aiProperties.getEmbedding().getDimension())
                .metadataKeys(VECTOR_METADATA_KEYS)
                .build();
    }

    public boolean indexHasMetadataFields() {
        try (JedisPooled jedis = jedisClient()) {
            Map<String, Object> info = jedis.ftInfo(indexName);
            return containsMetadataField(info.get("attributes"), "userId")
                    && containsMetadataField(info.get("attributes"), "shared");
        } catch (JedisDataException e) {
            return false;
        }
    }

    public void dropVectorIndex(boolean deleteDocuments) {
        try (JedisPooled jedis = jedisClient()) {
            if (deleteDocuments) {
                jedis.ftDropIndexDD(indexName);
            } else {
                jedis.ftDropIndex(indexName);
            }
            log.info("Dropped Redis vector index {} (deleteDocuments={})", indexName, deleteDocuments);
        } catch (JedisDataException e) {
            if (!Objects.toString(e.getMessage(), "").toLowerCase().contains("unknown index")) {
                throw e;
            }
        }
    }

    private JedisPooled jedisClient() {
        return new JedisPooled(redisHostPort.host(), redisHostPort.port());
    }

    private boolean containsMetadataField(Object attributes, String fieldName) {
        if (!(attributes instanceof List<?> list)) {
            return false;
        }
        for (Object item : list) {
            if (fieldName.equals(String.valueOf(item))) {
                return true;
            }
        }
        return false;
    }

    public void upsertChunks(List<DocChunkEntity> chunks, String fileName, List<float[]> vectors, boolean shared) {
        String sharedValue = shared ? "true" : "false";
        for (int i = 0; i < chunks.size(); i++) {
            DocChunkEntity chunk = chunks.get(i);
            Metadata metadata = Metadata.from(Map.of(
                    "userId", chunk.getUserId().toString(),
                    "documentId", chunk.getDocumentId().toString(),
                    "chunkId", chunk.getId().toString(),
                    "fileName", fileName,
                    "shared", sharedValue
            ));
            TextSegment segment = TextSegment.from(chunk.getContent(), metadata);
            Embedding embedding = new Embedding(vectors.get(i));
            embeddingStore.add(embedding, segment);
        }
    }

    public void removeByChunkIds(List<Long> chunkIds) {
        for (Long chunkId : chunkIds) {
            try {
                embeddingStore.removeAll(
                        MetadataFilterBuilder.metadataKey("chunkId").isEqualTo(chunkId.toString()));
            } catch (Exception e) {
                log.warn("Failed to remove vector for chunk {}: {}", chunkId, e.getMessage());
            }
        }
    }

    public void removeByDocumentId(Long documentId) {
        try {
            embeddingStore.removeAll(
                    MetadataFilterBuilder.metadataKey("documentId").isEqualTo(documentId.toString()));
        } catch (Exception e) {
            log.warn("Failed to remove vectors for document {}: {}", documentId, e.getMessage());
        }
    }

    public List<ScoredChunk> search(Long userId, Long documentId, float[] queryVector, int topK) {
        Embedding query = new Embedding(queryVector);
        int fetchK = documentId != null ? Math.max(topK * 30, 100) : topK * 5;
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(query)
                .maxResults(fetchK)
                .minScore(0.0)
                .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<ScoredChunk> matches = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : result.matches()) {
            Metadata md = match.embedded().metadata();
            String mdDocId = md.getString("documentId");
            if (documentId != null && !documentId.toString().equals(mdDocId)) {
                continue;
            }
            if (!isAccessible(userId, md)) {
                continue;
            }
            matches.add(new ScoredChunk(
                    Long.parseLong(md.getString("chunkId")),
                    Long.parseLong(mdDocId),
                    md.getString("fileName"),
                    match.embedded().text(),
                    match.score()
            ));
            if (matches.size() >= topK) {
                break;
            }
        }
        return matches;
    }

    private boolean isAccessible(Long userId, Metadata md) {
        if (userId.toString().equals(md.getString("userId"))) {
            return true;
        }
        return "true".equals(md.getString("shared"));
    }

    public static boolean isSharedUploader(String uploaderRole) {
        return UserRole.TEACHER.name().equals(uploaderRole);
    }

    public record ScoredChunk(Long chunkId, Long documentId, String fileName, String content, double score) {
    }

    public double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
