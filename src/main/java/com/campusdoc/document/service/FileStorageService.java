package com.campusdoc.document.service;

import com.campusdoc.config.FileStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
}
