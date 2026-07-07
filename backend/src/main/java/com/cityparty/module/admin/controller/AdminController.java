package com.cityparty.module.admin.controller;

import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.module.admin.service.AdminService;
import com.cityparty.module.admin.vo.CreditUserVO;
import com.cityparty.module.admin.vo.DashboardVO;
import com.cityparty.module.report.entity.Report;
import com.cityparty.module.signup.vo.SignupVO;
import com.cityparty.module.user.vo.UserMeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
    public Result<PageResult<UserMeVO>> users(@RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") Long current,
                                              @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.users(keyword, current, size));
    }

    @Operation(summary = "活动管理列表")
    @GetMapping("/activities")
    public Result<PageResult<?>> activities(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") Long current,
                                            @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.activities(keyword, category, status, current, size));
    }

    @Operation(summary = "报名管理列表")
    @GetMapping("/signups")
    public Result<PageResult<SignupVO>> signups(@RequestParam(required = false) Long activityId,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") Long current,
                                                @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.signups(activityId, status, current, size));
    }

    @Operation(summary = "信用分管理列表，第一阶段只展示")
    @GetMapping("/credits")
    public Result<PageResult<CreditUserVO>> credits(@RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "1") Long current,
                                                    @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.credits(keyword, current, size));
    }

    @Operation(summary = "举报管理列表，第一阶段预留")
    @GetMapping("/reports")
    public Result<PageResult<Report>> reports(@RequestParam(defaultValue = "1") Long current,
                                              @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(adminService.reports(current, size));
    }
}
