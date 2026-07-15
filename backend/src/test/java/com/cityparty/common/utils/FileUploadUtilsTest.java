package com.cityparty.common.utils;

import com.cityparty.common.config.UploadProperties;
import com.cityparty.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

    @Test
    void acceptsCroppedJpegWithExpectedDimensions() throws Exception {
        FileUploadUtils uploadUtils = new FileUploadUtils(uploadProperties());
        MockMultipartFile file = jpegFile("cover.jpg", 1200, 500);

        String url = uploadUtils.uploadCroppedJpeg(file, "activity", 1200, 500);

        assertThat(url).matches("/uploads/activity/[0-9a-f-]+\\.jpg");
        String filename = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve("activity").resolve(filename))).isTrue();
    }

    @Test
    void rejectsCroppedJpegWithUnexpectedDimensions() throws Exception {
        FileUploadUtils uploadUtils = new FileUploadUtils(uploadProperties());
        MockMultipartFile file = jpegFile("cover.jpg", 800, 600);

        assertThatThrownBy(() -> uploadUtils.uploadCroppedJpeg(file, "activity", 1200, 500))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1200×500");
    }

    @Test
    void deletesOnlyManagedUuidFilesInsideExpectedDirectory() throws Exception {
        FileUploadUtils uploadUtils = new FileUploadUtils(uploadProperties());
        String url = uploadUtils.uploadCroppedJpeg(jpegFile("avatar.jpg", 512, 512), "avatar", 512, 512);
        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path stored = tempDir.resolve("avatar").resolve(filename);
        Path unmanaged = tempDir.resolve("avatar").resolve("manual.jpg");
        Files.writeString(unmanaged, "keep");

        assertThat(uploadUtils.deleteManagedFile("https://example.com/avatar.jpg", "avatar")).isFalse();
        assertThat(uploadUtils.deleteManagedFile("/uploads/avatar/../../outside.jpg", "avatar")).isFalse();
        assertThat(uploadUtils.deleteManagedFile("/uploads/avatar/manual.jpg", "avatar")).isFalse();
        assertThat(Files.exists(unmanaged)).isTrue();
        assertThat(uploadUtils.deleteManagedFile(url, "avatar")).isTrue();
        assertThat(Files.exists(stored)).isFalse();
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

    private MockMultipartFile jpegFile(String filename, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.GRAY);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        return new MockMultipartFile("file", filename, "image/jpeg", output.toByteArray());
    }
}
