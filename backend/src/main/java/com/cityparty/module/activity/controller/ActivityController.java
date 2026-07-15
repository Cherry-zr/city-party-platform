package com.cityparty.module.activity.controller;

import com.cityparty.common.config.UploadProperties;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.common.utils.FileUploadUtils;
import com.cityparty.module.activity.dto.ActivityCreateDTO;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.signup.service.SignupService;
import com.cityparty.module.signup.vo.SignupVO;
import com.cityparty.module.waitlist.service.ActivityWaitlistService;
import com.cityparty.module.waitlist.vo.ActivityWaitlistVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Objects;

@Tag(name = "活动")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final SignupService signupService;
    private final ActivityWaitlistService waitlistService;
    private final FileUploadUtils fileUploadUtils;
    private final UploadProperties uploadProperties;

    @Operation(summary = "发布活动")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<ActivityVO> create(@Valid @RequestBody ActivityCreateDTO dto) {
        return Result.ok(activityService.create(dto));
    }

    @Operation(summary = "发布活动（包含裁剪封面）")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ActivityVO> createWithCover(@Valid @RequestPart("data") ActivityCreateDTO dto,
                                              @RequestPart(value = "cover", required = false) MultipartFile cover) {
        activityService.validateCreateRequest(dto);
        dto.setCoverUrl(null);
        String uploadedUrl = null;
        try {
            if (hasFile(cover)) {
                uploadedUrl = fileUploadUtils.uploadCroppedJpeg(
                        cover,
                        uploadProperties.getActivityDir(),
                        1200,
                        500
                );
                dto.setCoverUrl(uploadedUrl);
            }
            return Result.ok(activityService.create(dto));
        } catch (RuntimeException e) {
            fileUploadUtils.deleteManagedFile(uploadedUrl, uploadProperties.getActivityDir());
            throw e;
        }
    }

    @Operation(summary = "Edit activity")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<ActivityVO> update(@PathVariable Long id, @Valid @RequestBody ActivityCreateDTO dto) {
        String previousUrl = activityService.requireActivity(id).getCoverUrl();
        ActivityVO updated = activityService.update(id, dto);
        deleteReplacedCover(previousUrl, updated.getCoverUrl());
        return Result.ok(updated);
    }

    @Operation(summary = "编辑活动（包含裁剪封面）")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ActivityVO> updateWithCover(@PathVariable Long id,
                                              @Valid @RequestPart("data") ActivityCreateDTO dto,
                                              @RequestPart(value = "cover", required = false) MultipartFile cover,
                                              @RequestParam(defaultValue = "false") boolean removeCover) {
        if (hasFile(cover) && removeCover) {
            throw new BusinessException("不能同时上传和删除活动封面");
        }
        Activity current = activityService.validateUpdateRequest(id, dto);
        String previousUrl = current.getCoverUrl();
        String uploadedUrl = null;
        try {
            if (hasFile(cover)) {
                uploadedUrl = fileUploadUtils.uploadCroppedJpeg(
                        cover,
                        uploadProperties.getActivityDir(),
                        1200,
                        500
                );
                dto.setCoverUrl(uploadedUrl);
            } else if (removeCover) {
                dto.setCoverUrl(null);
            } else {
                dto.setCoverUrl(previousUrl);
            }
            ActivityVO updated = activityService.update(id, dto);
            deleteReplacedCover(previousUrl, updated.getCoverUrl());
            return Result.ok(updated);
        } catch (RuntimeException e) {
            fileUploadUtils.deleteManagedFile(uploadedUrl, uploadProperties.getActivityDir());
            throw e;
        }
    }

    @Operation(summary = "Cancel activity")
    @PatchMapping("/{id}/cancel")
    public Result<ActivityVO> cancel(@PathVariable Long id) {
        return Result.ok(activityService.cancel(id));
    }

    @Operation(summary = "Finish activity")
    @PatchMapping("/{id}/finish")
    public Result<ActivityVO> finish(@PathVariable Long id) {
        return Result.ok(activityService.finish(id));
    }

    @Operation(summary = "活动列表")
    @GetMapping
    public Result<PageResult<ActivityVO>> page(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String tag,
                                               @RequestParam(required = false) String city,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "1") Long current,
                                               @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(activityService.page(keyword, category, tag, city, status, current, size));
    }

    @Operation(summary = "附近活动列表")
    @GetMapping("/nearby")
    public Result<PageResult<ActivityVO>> nearby(@RequestParam(required = false) BigDecimal longitude,
                                                 @RequestParam(required = false) BigDecimal latitude,
                                                 @RequestParam(required = false) BigDecimal distanceKm,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) String tag,
                                                 @RequestParam(required = false) String city,
                                                 @RequestParam(defaultValue = "1") Long current,
                                                 @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(activityService.nearby(longitude, latitude, distanceKm, category, tag, city, current, size));
    }

    @Operation(summary = "活动详情")
    @GetMapping("/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return Result.ok(activityService.detail(id));
    }

    @Operation(summary = "我的活动")
    @GetMapping("/my")
    public Result<PageResult<ActivityVO>> myActivities(@RequestParam(defaultValue = "published") String type,
                                                       @RequestParam(defaultValue = "1") Long current,
                                                       @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(activityService.myActivities(type, current, size));
    }

    @Operation(summary = "活动报名列表，发起人可查看")
    @GetMapping("/{id}/signups")
    public Result<PageResult<SignupVO>> activitySignups(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "1") Long current,
                                                        @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(signupService.activitySignups(id, current, size));
    }

    @Operation(summary = "加入活动候补队列")
    @PostMapping("/{id}/waitlist")
    public Result<ActivityWaitlistVO> joinWaitlist(@PathVariable Long id) {
        return Result.ok(waitlistService.joinWaitlist(id));
    }

    @Operation(summary = "取消活动候补")
    @PostMapping("/{id}/waitlist/cancel")
    public Result<ActivityWaitlistVO> cancelWaitlist(@PathVariable Long id) {
        return Result.ok(waitlistService.cancelWaitlist(id));
    }

    @Operation(summary = "活动候补列表，发起人或管理员可查看")
    @GetMapping("/{id}/waitlist")
    public Result<PageResult<ActivityWaitlistVO>> waitlist(@PathVariable Long id,
                                                           @RequestParam(defaultValue = "1") Long current,
                                                           @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(waitlistService.listWaitlist(id, current, size));
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private void deleteReplacedCover(String previousUrl, String currentUrl) {
        if (!Objects.equals(previousUrl, currentUrl)) {
            fileUploadUtils.deleteManagedFile(previousUrl, uploadProperties.getActivityDir());
        }
    }
}
