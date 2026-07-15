package com.cityparty.common.utils;

import com.cityparty.common.config.UploadProperties;
import com.cityparty.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
public class FileUploadUtils {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Pattern MANAGED_FILENAME = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|jpeg|png|webp)$"
    );

    private final UploadProperties uploadProperties;

    public FileUploadUtils(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public String upload(MultipartFile file, String bizDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择上传文件");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 JPG、PNG、WebP 图片");
        }
        validateImageContent(file, extension);
        return store(file, bizDir, extension);
    }

    public String uploadCroppedJpeg(MultipartFile file, String bizDir, int expectedWidth, int expectedHeight) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择上传文件");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!Set.of("jpg", "jpeg").contains(extension)) {
            throw new BusinessException("裁剪后的图片必须为 JPEG 格式");
        }
        validateImageContent(file, extension);
        validateImageDimensions(file, expectedWidth, expectedHeight);
        return store(file, bizDir, "jpg");
    }

    public boolean deleteManagedFile(String url, String bizDir) {
        if (url == null || url.isBlank() || bizDir == null || bizDir.isBlank()) {
            return false;
        }
        String expectedPrefix = uploadProperties.getPublicPrefix() + "/" + bizDir + "/";
        if (!url.startsWith(expectedPrefix)) {
            return false;
        }
        String filename = url.substring(expectedPrefix.length());
        if (!MANAGED_FILENAME.matcher(filename).matches()) {
            return false;
        }
        Path uploadRoot = Path.of(uploadProperties.getBaseDir()).toAbsolutePath().normalize();
        Path bizRoot = uploadRoot.resolve(bizDir).normalize();
        Path target = bizRoot.resolve(filename).normalize();
        if (!bizRoot.startsWith(uploadRoot) || !target.getParent().equals(bizRoot)) {
            return false;
        }
        try {
            return Files.deleteIfExists(target);
        } catch (Exception e) {
            log.warn("Failed to delete managed upload: {}", target, e);
            return false;
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException("文件名缺少扩展名");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private void validateImageContent(MultipartFile file, String extension) {
        try {
            byte[] header = file.getInputStream().readNBytes(12);
            boolean valid = switch (extension) {
                case "jpg", "jpeg" -> isJpeg(header);
                case "png" -> isPng(header);
                case "webp" -> isWebp(header);
                default -> false;
            };
            if (!valid) {
                throw new BusinessException("Invalid image content.");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Invalid image content.");
        }
    }

    private void validateImageDimensions(MultipartFile file, int expectedWidth, int expectedHeight) {
        ImageReader reader = null;
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                throw new BusinessException("无法读取裁剪后的图片");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException("无法读取裁剪后的图片");
            }
            reader = readers.next();
            reader.setInput(input, true, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (width != expectedWidth || height != expectedHeight) {
                throw new BusinessException(String.format(
                        "图片尺寸必须为 %d×%d 像素",
                        expectedWidth,
                        expectedHeight
                ));
            }
            BufferedImage image = reader.read(0);
            if (image == null) {
                throw new BusinessException("无法读取裁剪后的图片");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("无法读取裁剪后的图片");
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    private String store(MultipartFile file, String bizDir, String extension) {
        try {
            Path uploadRoot = Path.of(uploadProperties.getBaseDir()).toAbsolutePath().normalize();
            Path dir = uploadRoot.resolve(bizDir).normalize();
            if (!dir.startsWith(uploadRoot)) {
                throw new BusinessException("非法上传目录");
            }
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = dir.resolve(filename).normalize();
            file.transferTo(target);
            return uploadProperties.getPublicPrefix() + "/" + bizDir + "/" + filename;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "文件上传失败");
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        byte[] signature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }
}
