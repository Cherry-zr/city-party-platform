package com.cityparty.module.user.controller;

import com.cityparty.common.config.UploadProperties;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.Result;
import com.cityparty.common.security.UserContext;
import com.cityparty.common.utils.FileUploadUtils;
import com.cityparty.module.user.dto.UpdateProfileDTO;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.ProfileOverviewVO;
import com.cityparty.module.user.vo.PublicUserProfileVO;
import com.cityparty.module.user.vo.UserMeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Tag(name = "用户资料")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final FileUploadUtils fileUploadUtils;
    private final UploadProperties uploadProperties;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/user/me")
    public Result<UserMeVO> me() {
        return Result.ok(userService.getMe(UserContext.getUserId()));
    }

    @Operation(summary = "获取当前用户个人中心概览")
    @GetMapping("/user/profile-overview")
    public Result<ProfileOverviewVO> profileOverview() {
        return Result.ok(userService.profileOverview(UserContext.getUserId()));
    }

    @Operation(summary = "修改当前用户资料")
    @PutMapping(value = "/user/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<UserMeVO> updateProfile(@RequestBody UpdateProfileDTO dto) {
        Long userId = UserContext.getUserId();
        String previousUrl = userService.getMe(userId).getAvatarUrl();
        UserMeVO updated = userService.updateProfile(userId, dto);
        deleteReplacedAvatar(previousUrl, updated.getAvatarUrl());
        return Result.ok(updated);
    }

    @Operation(summary = "修改当前用户资料（包含裁剪头像）")
    @PutMapping(value = "/user/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserMeVO> updateProfileWithAvatar(@Valid @RequestPart("data") UpdateProfileDTO dto,
                                                    @RequestPart(value = "avatar", required = false) MultipartFile avatar,
                                                    @RequestParam(defaultValue = "false") boolean removeAvatar) {
        if (hasFile(avatar) && removeAvatar) {
            throw new BusinessException("不能同时上传和删除头像");
        }
        Long userId = UserContext.getUserId();
        String previousUrl = userService.getMe(userId).getAvatarUrl();
        String uploadedUrl = null;
        dto.setAvatarUrl(null);
        dto.setRemoveAvatar(removeAvatar);
        try {
            if (hasFile(avatar)) {
                uploadedUrl = fileUploadUtils.uploadCroppedJpeg(
                        avatar,
                        uploadProperties.getAvatarDir(),
                        512,
                        512
                );
                dto.setAvatarUrl(uploadedUrl);
            }
            UserMeVO updated = userService.updateProfile(userId, dto);
            deleteReplacedAvatar(previousUrl, updated.getAvatarUrl());
            return Result.ok(updated);
        } catch (RuntimeException e) {
            fileUploadUtils.deleteManagedFile(uploadedUrl, uploadProperties.getAvatarDir());
            throw e;
        }
    }

    @Operation(summary = "查看用户公开主页")
    @GetMapping("/users/{id}/public-profile")
    public Result<PublicUserProfileVO> publicProfile(@PathVariable Long id) {
        return Result.ok(userService.publicProfile(id));
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private void deleteReplacedAvatar(String previousUrl, String currentUrl) {
        if (!Objects.equals(previousUrl, currentUrl)) {
            fileUploadUtils.deleteManagedFile(previousUrl, uploadProperties.getAvatarDir());
        }
    }
}
