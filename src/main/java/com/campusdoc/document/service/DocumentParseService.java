package com.campusdoc.document.service;

import com.campusdoc.ai.client.EmbeddingClient;
import com.campusdoc.ai.service.VectorStoreService;
import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.document.entity.DocChunkEntity;
import com.campusdoc.document.entity.DocumentEntity;
import com.campusdoc.document.entity.DocumentStatus;
import com.campusdoc.document.mapper.DocChunkMapper;
import com.campusdoc.document.mapper.DocumentMapper;
import com.campusdoc.user.entity.UserEntity;
import com.campusdoc.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentParseService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseService.class);

    private final DocumentMapper documentMapper;
    private final DocChunkMapper docChunkMapper;
    private final DocumentTextExtractor textExtractor;
    private final TextChunker textChunker;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;
    private final UserMapper userMapper;

    public DocumentParseService(DocumentMapper documentMapper,
                                DocChunkMapper docChunkMapper,
                                DocumentTextExtractor textExtractor,
                                TextChunker textChunker,
                                EmbeddingClient embeddingClient,
                                VectorStoreService vectorStoreService,
                                UserMapper userMapper) {
        this.documentMapper = documentMapper;
        this.docChunkMapper = docChunkMapper;
        this.textExtractor = textExtractor;
        this.textChunker = textChunker;
        this.embeddingClient = embeddingClient;
        this.vectorStoreService = vectorStoreService;
        this.userMapper = userMapper;
    }

    @Async("documentParseExecutor")
    public void parseAsync(Long documentId) {
        try {
            parse(documentId);
        } catch (Exception e) {
            log.error("Parse document {} failed", documentId, e);
            String msg = e instanceof BusinessException be ? be.resolvedMessage() : e.getMessage();
            documentMapper.updateStatus(documentId, DocumentStatus.FAILED, truncate(msg));
        }
    }

    public void parse(Long documentId) {
        DocumentEntity doc = documentMapper.findById(documentId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        documentMapper.updateStatus(documentId, DocumentStatus.PARSING, null);
        String extension = extensionOf(doc.getFileName());
        String text = textExtractor.extract(Path.of(doc.getStoragePath()), extension);
        List<String> parts = textChunker.chunk(text);
        List<DocChunkEntity> oldChunks = docChunkMapper.findByDocumentId(documentId);
        if (!oldChunks.isEmpty()) {
            vectorStoreService.removeByChunkIds(oldChunks.stream().map(DocChunkEntity::getId).toList());
        }
        docChunkMapper.deleteByDocumentId(documentId);

        List<DocChunkEntity> entities = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            DocChunkEntity chunk = new DocChunkEntity();
            chunk.setDocumentId(documentId);
            chunk.setUserId(doc.getUserId());
            chunk.setChunkIndex(i);
            chunk.setContent(parts.get(i));
            entities.add(chunk);
        }
        if (!entities.isEmpty()) {
            docChunkMapper.batchInsert(entities);
        }
        List<DocChunkEntity> saved = docChunkMapper.findByDocumentId(documentId);
        if (!saved.isEmpty()) {
            List<float[]> vectors = embeddingClient.embedBatch(saved.stream().map(DocChunkEntity::getContent).toList());
            boolean shared = isTeacherUploader(doc.getUserId());
            vectorStoreService.upsertChunks(saved, doc.getFileName(), vectors, shared);
        }
        documentMapper.updateStatus(documentId, DocumentStatus.SUCCESS, null);
    }

    private String extensionOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(idx + 1) : "";
    }

    private boolean isTeacherUploader(Long userId) {
        UserEntity user = userMapper.findById(userId);
        return user != null && com.campusdoc.user.entity.UserRole.TEACHER == user.getRole();
    }

    private String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
