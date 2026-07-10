package com.cityparty.common.utils;

import com.cityparty.common.config.UploadProperties;
import com.cityparty.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Component
public class FileUploadUtils {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

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
        try {
            Path dir = Path.of(uploadProperties.getBaseDir(), bizDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return uploadProperties.getPublicPrefix() + "/" + bizDir + "/" + filename;
        } catch (Exception e) {
            throw new BusinessException(500, "文件上传失败");
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
