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
}
