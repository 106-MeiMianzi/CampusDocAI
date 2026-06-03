package com.campusdoc.ai.service;

import com.campusdoc.ai.client.EmbeddingClient;
import com.campusdoc.document.entity.DocChunkEntity;
import com.campusdoc.document.entity.DocChunkVectorRow;
import com.campusdoc.document.mapper.DocChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VectorReindexService {

    private static final Logger log = LoggerFactory.getLogger(VectorReindexService.class);
    private static final int EMBED_BATCH_SIZE = 20;

    private final DocChunkMapper docChunkMapper;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;

    public VectorReindexService(DocChunkMapper docChunkMapper,
                                EmbeddingClient embeddingClient,
                                VectorStoreService vectorStoreService) {
        this.docChunkMapper = docChunkMapper;
        this.embeddingClient = embeddingClient;
        this.vectorStoreService = vectorStoreService;
    }

    public int reindexFromChunkTable() {
        List<DocChunkVectorRow> rows = docChunkMapper.listForVectorIndex();
        if (rows.isEmpty()) {
            return 0;
        }
        Map<Long, List<DocChunkVectorRow>> byDocument = new LinkedHashMap<>();
        for (DocChunkVectorRow row : rows) {
            byDocument.computeIfAbsent(row.getDocumentId(), ignored -> new ArrayList<>()).add(row);
        }
        int chunkCount = 0;
        for (List<DocChunkVectorRow> group : byDocument.values()) {
            chunkCount += reindexGroup(group);
        }
        log.info("Vector reindex from doc_chunk finished: {} chunks in {} documents",
                chunkCount, byDocument.size());
        return chunkCount;
    }

    private int reindexGroup(List<DocChunkVectorRow> rows) {
        String fileName = rows.get(0).getFileName();
        List<DocChunkEntity> chunks = rows.stream().map(this::toChunkEntity).toList();
        for (int from = 0; from < chunks.size(); from += EMBED_BATCH_SIZE) {
            int to = Math.min(from + EMBED_BATCH_SIZE, chunks.size());
            List<DocChunkEntity> batch = chunks.subList(from, to);
            List<float[]> vectors = embeddingClient.embedBatch(
                    batch.stream().map(DocChunkEntity::getContent).toList());
            boolean shared = VectorStoreService.isSharedUploader(rows.get(0).getUploaderRole());
            vectorStoreService.upsertChunks(batch, fileName, vectors, shared);
        }
        return chunks.size();
    }

    private DocChunkEntity toChunkEntity(DocChunkVectorRow row) {
        DocChunkEntity chunk = new DocChunkEntity();
        chunk.setId(row.getId());
        chunk.setDocumentId(row.getDocumentId());
        chunk.setUserId(row.getUserId());
        chunk.setChunkIndex(row.getChunkIndex());
        chunk.setContent(row.getContent());
        return chunk;
    }
}
