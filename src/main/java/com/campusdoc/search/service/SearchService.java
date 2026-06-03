package com.campusdoc.search.service;

import com.campusdoc.ai.client.EmbeddingClient;
import com.campusdoc.ai.service.VectorStoreService;
import com.campusdoc.document.entity.DocChunkEntity;
import com.campusdoc.document.mapper.DocChunkMapper;
import com.campusdoc.document.mapper.DocumentMapper;
import com.campusdoc.document.entity.DocumentEntity;
import com.campusdoc.search.dto.SearchHitResponse;
import com.campusdoc.search.dto.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final DocChunkMapper docChunkMapper;
    private final DocumentMapper documentMapper;
    private final int defaultTopK;

    public SearchService(EmbeddingClient embeddingClient,
                         VectorStoreService vectorStoreService,
                         DocChunkMapper docChunkMapper,
                         DocumentMapper documentMapper,
                         @Value("${search.default-top-k:5}") int defaultTopK) {
        this.embeddingClient = embeddingClient;
        this.vectorStoreService = vectorStoreService;
        this.docChunkMapper = docChunkMapper;
        this.documentMapper = documentMapper;
        this.defaultTopK = defaultTopK;
    }

    public List<SearchHitResponse> semantic(Long userId, SearchRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;
        float[] vector = embeddingClient.embed(request.getQuestion());
        List<VectorStoreService.ScoredChunk> hits = vectorStoreService.search(userId, null, vector, topK);
        return hits.stream()
                .map(h -> new SearchHitResponse(
                        h.chunkId(), h.documentId(), h.fileName(), h.score(), h.content()))
                .toList();
    }

    public List<SearchHitResponse> keyword(Long userId, SearchRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;
        String keyword = request.getKeyword() != null && !request.getKeyword().isBlank()
                ? request.getKeyword() : request.getQuestion();
        List<DocChunkEntity> chunks = docChunkMapper.keywordSearch(userId, null, keyword, topK);
        Map<Long, String> fileNames = chunks.stream()
                .map(DocChunkEntity::getDocumentId)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> {
                    DocumentEntity doc = documentMapper.findById(id);
                    return doc != null ? doc.getFileName() : "";
                }));
        List<SearchHitResponse> result = new ArrayList<>();
        for (DocChunkEntity chunk : chunks) {
            result.add(new SearchHitResponse(
                    chunk.getId(),
                    chunk.getDocumentId(),
                    fileNames.getOrDefault(chunk.getDocumentId(), ""),
                    1.0,
                    chunk.getContent()));
        }
        return result;
    }
}
