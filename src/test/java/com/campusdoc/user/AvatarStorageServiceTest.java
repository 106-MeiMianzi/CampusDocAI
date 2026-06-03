package com.campusdoc.user;

import com.campusdoc.config.FileStorageProperties;
import com.campusdoc.user.service.AvatarStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AvatarStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveFromDataUrlStoresFileAndReturnsPublicPath() throws Exception {
        FileStorageProperties props = new FileStorageProperties();
        props.setUploadPath(tempDir.toString() + "/");
        AvatarStorageService service = new AvatarStorageService(props);

        String dataUrl = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47
        });
        String publicPath = service.saveFromDataUrl(7L, dataUrl);

        assertThat(publicPath).isEqualTo("/api/user/avatar/image/7");
        assertThat(service.load(7L)).isPresent();
        assertThat(Files.list(tempDir.resolve("7/avatar")).findAny()).isPresent();
    }
}
