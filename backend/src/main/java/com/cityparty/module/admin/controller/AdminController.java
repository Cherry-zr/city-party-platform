package com.cityparty.module.admin.controller;

import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.admin.service.AdminService;
import com.cityparty.module.admin.vo.AdminCreditRecordVO;
import com.cityparty.module.admin.vo.AdminNoticeVO;
import com.cityparty.module.admin.vo.AdminUserVO;
import com.cityparty.module.admin.vo.DashboardVO;
import com.cityparty.module.report.entity.Report;
import com.cityparty.module.review.vo.ActivityReviewVO;
import com.cityparty.module.signup.vo.SignupVO;
import com.cityparty.module.waitlist.vo.ActivityWaitlistVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员后台")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "后台数据看板")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.ok(adminService.dashboard());
    }

    @Operation(summary = "用户管理列表")
    @GetMapping("/users")
    public Result<PageResult<AdminUserVO>> users(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Long current,
                                                 @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.users(keyword, current, size));
    }

    @Operation(summary = "用户管理详情")
    @GetMapping("/users/{id}")
    public Result<AdminUserVO> userDetail(@PathVariable Long id) {
        return Result.ok(adminService.userDetail(id));
    }

    @Operation(summary = "活动管理列表")
    @GetMapping("/activities")
    public Result<PageResult<ActivityVO>> activities(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String category,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") Long current,
                                                      @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.activities(keyword, category, status, current, size));
    }

    @Operation(summary = "活动管理详情")
    @GetMapping("/activities/{id}")
    public Result<ActivityVO> activityDetail(@PathVariable Long id) {
        return Result.ok(adminService.activityDetail(id));
    }

    @Operation(summary = "活动报名用户")
    @GetMapping("/activities/{id}/signups")
    public Result<PageResult<SignupVO>> activitySignups(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "1") Long current,
                                                        @RequestParam(defaultValue = "20") Long size) {
        return Result.ok(adminService.activitySignups(id, current, size));
    }

    @Operation(summary = "活动候补用户")
    @GetMapping("/activities/{id}/waitlist")
    public Result<PageResult<ActivityWaitlistVO>> activityWaitlist(@PathVariable Long id,
                                                                   @RequestParam(defaultValue = "1") Long current,
                                                                   @RequestParam(defaultValue = "20") Long size) {
        return Result.ok(adminService.activityWaitlist(id, current, size));
    }

    @Operation(summary = "报名管理列表")
    @GetMapping("/signups")
    public Result<PageResult<SignupVO>> signups(@RequestParam(required = false) Long activityId,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") Long current,
                                                @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.signups(activityId, status, current, size));
    }

    @Operation(summary = "评价管理列表")
    @GetMapping("/reviews")
    public Result<PageResult<ActivityReviewVO>> reviews(@RequestParam(required = false) Long activityId,
                                                        @RequestParam(required = false) Long userId,
                                                        @RequestParam(defaultValue = "1") Long current,
                                                        @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.reviews(activityId, userId, current, size));
    }

    @Operation(summary = "信用变化明细")
    @GetMapping("/credits")
    public Result<PageResult<AdminCreditRecordVO>> credits(@RequestParam(required = false) Long userId,
                                                           @RequestParam(defaultValue = "1") Long current,
                                                           @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.credits(userId, current, size));
    }

    @Operation(summary = "系统通知记录")
    @GetMapping("/notices")
    public Result<PageResult<AdminNoticeVO>> notices(@RequestParam(required = false) Long userId,
                                                     @RequestParam(defaultValue = "1") Long current,
                                                     @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.notices(userId, current, size));
    }

    @Operation(summary = "举报管理列表，保留第一阶段兼容接口")
    @GetMapping("/reports")
    public Result<PageResult<Report>> reports(@RequestParam(defaultValue = "1") Long current,
                                              @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.reports(current, size));
    }
}
