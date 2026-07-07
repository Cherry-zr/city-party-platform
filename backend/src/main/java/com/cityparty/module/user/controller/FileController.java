package com.cityparty.module.user.controller;

import com.cityparty.common.config.UploadProperties;
import com.cityparty.common.result.Result;
import com.cityparty.common.security.UserContext;
import com.cityparty.common.utils.FileUploadUtils;
import com.cityparty.module.user.dto.UpdateProfileDTO;
import com.cityparty.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "文件上传")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
public class FileController {

    private final FileUploadUtils fileUploadUtils;
    private final UploadProperties uploadProperties;
    private final UserService userService;

    @Operation(summary = "上传用户头像")
    @PostMapping("/upload/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = fileUploadUtils.upload(file, uploadProperties.getAvatarDir());
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setAvatarUrl(url);
        userService.updateProfile(UserContext.getUserId(), dto);
        return Result.ok(Map.of("url", url));
    }

    @Operation(summary = "上传活动封面")
    @PostMapping("/upload/activity-cover")
    public Result<Map<String, String>> uploadActivityCover(@RequestParam("file") MultipartFile file) {
        String url = fileUploadUtils.upload(file, uploadProperties.getActivityDir());
        return Result.ok(Map.of("url", url));
    }
}
