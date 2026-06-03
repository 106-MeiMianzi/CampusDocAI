package com.campusdoc.document.service;

import com.campusdoc.ai.client.EmbeddingClient;
import com.campusdoc.ai.service.VectorStoreService;
import com.campusdoc.document.entity.DocChunkEntity;
import com.campusdoc.document.mapper.DocChunkMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 单文档问答：Redis 向量库不支持按 documentId 过滤检索，改为对该文档切块重新 embedding 后做余弦相似度排序。
 */
@Service
public class DocumentScopedSearchService {

    private final DocChunkMapper docChunkMapper;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;

    public DocumentScopedSearchService(DocChunkMapper docChunkMapper,
                                       EmbeddingClient embeddingClient,
                                       VectorStoreService vectorStoreService) {
        this.docChunkMapper = docChunkMapper;
        this.embeddingClient = embeddingClient;
        this.vectorStoreService = vectorStoreService;
    }

    public List<VectorStoreService.ScoredChunk> search(Long documentId, String fileName,
                                                       float[] queryVector, int topK) {
        List<DocChunkEntity> chunks = docChunkMapper.findByDocumentId(documentId);
        if (chunks.isEmpty()) {
            return List.of();
        }
        List<String> texts = chunks.stream().map(DocChunkEntity::getContent).toList();
        List<float[]> chunkVectors = embeddingClient.embedBatch(texts);
        List<VectorStoreService.ScoredChunk> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            DocChunkEntity chunk = chunks.get(i);
            double score = vectorStoreService.cosineSimilarity(queryVector, chunkVectors.get(i));
            scored.add(new VectorStoreService.ScoredChunk(
                    chunk.getId(), documentId, fileName, chunk.getContent(), score));
        }
        scored.sort(Comparator.comparingDouble(VectorStoreService.ScoredChunk::score).reversed());
        return scored.size() <= topK ? scored : scored.subList(0, topK);
    }
}
