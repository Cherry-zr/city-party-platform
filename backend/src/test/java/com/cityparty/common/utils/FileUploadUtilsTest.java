package com.cityparty.common.utils;

import com.cityparty.common.config.UploadProperties;
import com.cityparty.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsPngContentWithPngExtension() {
        FileUploadUtils uploadUtils = new FileUploadUtils(uploadProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                pngBytes()
        );

        String url = uploadUtils.upload(file, "avatar");

        assertThat(url).startsWith("/uploads/avatar/");
        String filename = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve("avatar").resolve(filename))).isTrue();
    }

    @Test
    void rejectsNonImageContentWithImageExtension() {
        FileUploadUtils uploadUtils = new FileUploadUtils(uploadProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "not an image".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> uploadUtils.upload(file, "avatar"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid image content");
    }

    @Test
    void rejectsEmptyFile() {
        FileUploadUtils uploadUtils = new FileUploadUtils(uploadProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> uploadUtils.upload(file, "avatar"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsDisguisedImageWithUnsupportedExtension() {
        FileUploadUtils uploadUtils = new FileUploadUtils(uploadProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.exe",
                "image/png",
                pngBytes()
        );

        assertThatThrownBy(() -> uploadUtils.upload(file, "avatar"))
                .isInstanceOf(BusinessException.class);
    }

    private UploadProperties uploadProperties() {
        UploadProperties properties = new UploadProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setPublicPrefix("/uploads");
        return properties;
    }

    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
    }
}
