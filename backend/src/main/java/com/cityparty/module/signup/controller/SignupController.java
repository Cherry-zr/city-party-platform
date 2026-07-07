package com.cityparty.module.signup.controller;

import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.module.signup.dto.SignupCreateDTO;
import com.cityparty.module.signup.dto.SignupReviewDTO;
import com.cityparty.module.signup.service.SignupService;
import com.cityparty.module.signup.vo.SignupVO;
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

@Tag(name = "活动报名")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SignupController {

    private final SignupService signupService;

    @Operation(summary = "报名活动")
    @PostMapping("/activities/{activityId}/signup")
    public Result<SignupVO> signup(@PathVariable Long activityId, @RequestBody(required = false) SignupCreateDTO dto) {
        return Result.ok(signupService.signup(activityId, dto));
    }

    @Operation(summary = "报名活动（兼容接口）")
    @PostMapping("/signups")
    public Result<SignupVO> signupCompat(@RequestBody SignupCreateDTO dto) {
        if (dto == null || dto.getActivityId() == null) {
            throw new BusinessException(400, "activityId 不能为空");
        }
        return Result.ok(signupService.signup(dto.getActivityId(), dto));
    }

    @Operation(summary = "退出活动")
    @PostMapping("/activities/{activityId}/signup/cancel")
    public Result<SignupVO> cancel(@PathVariable Long activityId) {
        return Result.ok(signupService.cancel(activityId));
    }

    @Operation(summary = "审核报名")
    @PostMapping("/signups/{signupId}/review")
    public Result<SignupVO> review(@PathVariable Long signupId, @Valid @RequestBody SignupReviewDTO dto) {
        return Result.ok(signupService.review(signupId, dto.getStatus()));
    }

    @Operation(summary = "通过报名（兼容接口）")
    @PostMapping("/signups/{signupId}/approve")
    public Result<SignupVO> approve(@PathVariable Long signupId) {
        return Result.ok(signupService.review(signupId, "APPROVED"));
    }

    @Operation(summary = "拒绝报名（兼容接口）")
    @PostMapping("/signups/{signupId}/reject")
    public Result<SignupVO> reject(@PathVariable Long signupId) {
        return Result.ok(signupService.review(signupId, "REJECTED"));
    }

    @Operation(summary = "我的报名")
    @GetMapping("/signups/my")
    public Result<PageResult<SignupVO>> mySignups(@RequestParam(defaultValue = "1") Long current,
                                                  @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(signupService.mySignups(current, size));
    }
}
