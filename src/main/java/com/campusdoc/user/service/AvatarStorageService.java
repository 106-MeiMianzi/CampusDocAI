package com.campusdoc.user.service;

import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.config.FileStorageProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class AvatarStorageService {

    private static final Pattern DATA_URL = Pattern.compile("^data:([^;,]+)(;base64)?,(.+)$", Pattern.DOTALL);
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp",
            "image/svg+xml", "svg"
    );

    private final FileStorageProperties properties;

    public AvatarStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    public String saveFromDataUrl(Long userId, String dataUrl) {
        Matcher matcher = DATA_URL.matcher(dataUrl.trim());
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像格式无效，请上传图片文件");
        }
        String mime = matcher.group(1).trim().toLowerCase();
        String extension = MIME_TO_EXT.get(mime);
        if (extension == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        byte[] bytes = decodePayload(matcher.group(2) != null, matcher.group(3));
        if (bytes.length > MAX_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        try {
            Path dir = avatarDir(userId);
            Files.createDirectories(dir);
            clearDir(dir);
            Path target = dir.resolve("current." + extension);
            Files.write(target, bytes);
            return publicAvatarPath(userId);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "头像保存失败");
        }
    }

    public Optional<StoredAvatar> load(Long userId) {
        Path dir = avatarDir(userId);
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .findFirst()
                    .map(path -> new StoredAvatar(path, mediaTypeFor(path)));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public boolean isLocalAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return false;
        }
        return avatarUrl.startsWith("/api/user/avatar/image/");
    }

    public void deleteIfLocal(Long userId) {
        Path dir = avatarDir(userId);
        if (!Files.isDirectory(dir)) {
            return;
        }
        clearDir(dir);
        try {
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
            // best effort
        }
    }

    public String publicAvatarPath(Long userId) {
        return "/api/user/avatar/image/" + userId;
    }

    private Path avatarDir(Long userId) {
        return Path.of(properties.getUploadPath(), String.valueOf(userId), "avatar");
    }

    private byte[] decodePayload(boolean base64, String payload) {
        if (base64) {
            try {
                return Base64.getDecoder().decode(payload.replaceAll("\\s", ""));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "头像 Base64 解码失败");
            }
        }
        return payload.getBytes();
    }

    private void clearDir(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException ignored) {
            // best effort
        }
    }

    private MediaType mediaTypeFor(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (fileName.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (fileName.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (fileName.endsWith(".svg")) {
            return MediaType.parseMediaType("image/svg+xml");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    public record StoredAvatar(Path path, MediaType mediaType) {

        public Resource asResource() {
            try {
                Resource resource = new UrlResource(path.toUri());
                if (!resource.exists() || !resource.isReadable()) {
                    throw new BusinessException(ErrorCode.NOT_FOUND);
                }
                return resource;
            } catch (MalformedURLException ex) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
        }
    }
}
