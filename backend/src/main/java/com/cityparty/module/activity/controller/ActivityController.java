package com.cityparty.module.activity.controller;

import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.module.activity.dto.ActivityCreateDTO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "活动")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final SignupService signupService;
    private final ActivityWaitlistService waitlistService;

    @Operation(summary = "发布活动")
    @PostMapping
    public Result<ActivityVO> create(@Valid @RequestBody ActivityCreateDTO dto) {
        return Result.ok(activityService.create(dto));
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
}
