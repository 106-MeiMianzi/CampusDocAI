package com.campusdoc.document.service;

import com.campusdoc.ai.service.VectorStoreService;
import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.common.PageResult;
import com.campusdoc.config.DocumentProperties;
import com.campusdoc.document.dto.DocumentDetailResponse;
import com.campusdoc.document.dto.DocumentListItemResponse;
import com.campusdoc.document.dto.DocumentUploadItemResponse;
import com.campusdoc.document.dto.StoredDocumentFile;
import com.campusdoc.document.entity.DocChunkEntity;
import com.campusdoc.document.entity.DocumentEntity;
import com.campusdoc.document.entity.DocumentStatus;
import com.campusdoc.document.mapper.DocChunkMapper;
import com.campusdoc.document.mapper.DocumentMapper;
import com.campusdoc.user.entity.UserEntity;
import com.campusdoc.user.entity.UserRole;
import com.campusdoc.user.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "docx", "xlsx");
    private static final Map<String, MediaType> EXT_TO_MEDIA_TYPE = Map.of(
            "pdf", MediaType.APPLICATION_PDF,
            "docx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "xlsx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    );

    private final DocumentMapper documentMapper;
    private final DocChunkMapper docChunkMapper;
    private final FileStorageService fileStorageService;
    private final DocumentParseService documentParseService;
    private final VectorStoreService vectorStoreService;
    private final DocumentProperties documentProperties;
    private final UserService userService;

    public DocumentService(DocumentMapper documentMapper,
                           DocChunkMapper docChunkMapper,
                           FileStorageService fileStorageService,
                           DocumentParseService documentParseService,
                           VectorStoreService vectorStoreService,
                           DocumentProperties documentProperties,
                           UserService userService) {
        this.documentMapper = documentMapper;
        this.docChunkMapper = docChunkMapper;
        this.fileStorageService = fileStorageService;
        this.documentParseService = documentParseService;
        this.vectorStoreService = vectorStoreService;
        this.documentProperties = documentProperties;
        this.userService = userService;
    }

    @Transactional
    public List<DocumentUploadItemResponse> upload(Long userId, List<MultipartFile> files) {
        requireTeacher(userId);
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传至少一个文件");
        }
        if (files.size() > documentProperties.getMaxFilesPerRequest()) {
            throw new BusinessException(ErrorCode.TOO_MANY_FILES);
        }
        long maxBytes = documentProperties.getMaxFileSizeMb() * 1024L * 1024L;
        List<DocumentUploadItemResponse> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.getSize() > maxBytes) {
                throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
            }
            String original = file.getOriginalFilename();
            String ext = extension(original);
            if (!ALLOWED_EXT.contains(ext)) {
                throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
            }
            try {
                Path path = fileStorageService.save(userId, file, ext);
                DocumentEntity entity = new DocumentEntity();
                entity.setUserId(userId);
                entity.setFileName(original != null ? original : "unknown." + ext);
                entity.setStoragePath(path.toString());
                entity.setFileSize(file.getSize());
                entity.setStatus(DocumentStatus.UPLOADING);
                documentMapper.insert(entity);
                documentMapper.updateStatus(entity.getId(), DocumentStatus.PARSING, null);
                documentParseService.parseAsync(entity.getId());
                result.add(new DocumentUploadItemResponse(entity.getId(), entity.getFileName()));
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败: " + e.getMessage());
            }
        }
        return result;
    }

    public PageResult<DocumentListItemResponse> list(Long userId, DocumentStatus status, int page, int size) {
        int offset = Math.max(page - 1, 0) * size;
        List<DocumentEntity> docs = documentMapper.listAccessible(userId, status, offset, size);
        long total = documentMapper.countAccessible(userId, status);
        List<DocumentListItemResponse> list = docs.stream()
                .map(d -> new DocumentListItemResponse(d.getId(), d.getFileName(), d.getStatus(), d.getCreatedAt()))
                .toList();
        return new PageResult<>(list, total, page, size);
    }

    public DocumentDetailResponse detail(Long userId, Long id) {
        DocumentEntity doc = requireAccessible(userId, id);
        return new DocumentDetailResponse(
                doc.getId(), doc.getFileName(), doc.getStatus(), doc.getFileSize(),
                doc.getErrorMsg(), doc.getCreatedAt(), doc.getUpdatedAt());
    }

    public StoredDocumentFile openFile(Long userId, Long id) {
        DocumentEntity doc = requireAccessible(userId, id);
        Resource resource = fileStorageService.loadAsResource(doc.getStoragePath());
        return new StoredDocumentFile(resource, mediaTypeFor(doc.getFileName()), doc.getFileName());
    }

    public void reparse(Long userId, Long id) {
        requireTeacher(userId);
        requireAccessible(userId, id);
        documentMapper.updateStatus(id, DocumentStatus.PARSING, null);
        documentParseService.parseAsync(id);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        DocumentEntity doc = requireAccessible(userId, id);
        requireDeletePermission(userId, doc);
        vectorStoreService.removeByDocumentId(id);
        docChunkMapper.deleteByDocumentId(id);
        if (doc.getUserId().equals(userId)) {
            documentMapper.deleteByIdAndUserId(id, userId);
        } else {
            documentMapper.deleteById(id);
        }
        fileStorageService.deleteIfExists(doc.getStoragePath());
    }

    public DocumentEntity requireAccessible(Long userId, Long id) {
        DocumentEntity doc = documentMapper.findAccessibleById(id, userId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return doc;
    }

    private void requireTeacher(Long userId) {
        UserEntity user = userService.requireById(userId);
        if (user.getRole() != UserRole.TEACHER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅教师可上传或管理文档");
        }
    }

    private void requireDeletePermission(Long userId, DocumentEntity doc) {
        if (doc.getUserId().equals(userId)) {
            return;
        }
        UserEntity user = userService.requireById(userId);
        if (user.getRole() != UserRole.TEACHER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该文档");
        }
    }

    private String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private MediaType mediaTypeFor(String fileName) {
        return EXT_TO_MEDIA_TYPE.getOrDefault(extension(fileName), MediaType.APPLICATION_OCTET_STREAM);
    }
}
