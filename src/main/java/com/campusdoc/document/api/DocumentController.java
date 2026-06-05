package com.campusdoc.document.api;

import com.campusdoc.common.ApiResponse;
import com.campusdoc.common.PageResult;
import com.campusdoc.document.dto.DocumentDetailResponse;
import com.campusdoc.document.dto.DocumentListItemResponse;
import com.campusdoc.document.dto.DocumentUploadItemResponse;
import com.campusdoc.document.dto.StoredDocumentFile;
import com.campusdoc.document.entity.DocumentStatus;
import com.campusdoc.document.service.DocumentService;
import com.campusdoc.security.SecurityUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ApiResponse<List<DocumentUploadItemResponse>> upload(@RequestParam("files") MultipartFile[] files) {
        List<MultipartFile> list = files == null ? List.of() : Arrays.asList(files);
        return ApiResponse.ok(documentService.upload(SecurityUtils.currentUserId(), list));
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<DocumentListItemResponse>> list(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(documentService.list(SecurityUtils.currentUserId(), status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(documentService.detail(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        StoredDocumentFile stored = documentService.openFile(SecurityUtils.currentUserId(), id);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(stored.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(stored.mediaType())
                .body(stored.resource());
    }

    @PostMapping("/{id}/reparse")
    public ApiResponse<Map<String, String>> reparse(@PathVariable Long id) {
        documentService.reparse(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok(Map.of("message", "已触发重新解析"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, String>> delete(@PathVariable Long id) {
        documentService.delete(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok(Map.of("message", "删除成功"));
    }
}
