package com.campusdoc.document.service;

import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.config.FileStorageProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class FileStorageService {

    private final FileStorageProperties properties;

    public FileStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    public Path save(Long userId, MultipartFile file, String extension) throws IOException {
        String month = YearMonth.now().toString().replace("-", "");
        Path dir = Path.of(properties.getUploadPath(), String.valueOf(userId), month);
        Files.createDirectories(dir);
        String fileName = UUID.randomUUID() + "." + extension;
        Path target = dir.resolve(fileName);
        file.transferTo(target);
        return target;
    }

    public void deleteIfExists(String storagePath) {
        if (storagePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(storagePath));
        } catch (IOException ignored) {
            // best effort
        }
    }

    public Resource loadAsResource(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        try {
            Path path = Path.of(storagePath);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
    }
}
