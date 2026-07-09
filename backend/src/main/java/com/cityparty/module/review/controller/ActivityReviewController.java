package com.cityparty.module.review.controller;

import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.module.review.dto.ReviewCreateDTO;
import com.cityparty.module.review.service.ActivityReviewService;
import com.cityparty.module.review.vo.ActivityReviewVO;
import com.cityparty.module.review.vo.ReviewTargetVO;
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

import java.util.List;

@Tag(name = "活动评价")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ActivityReviewController {

    private final ActivityReviewService reviewService;

    @Operation(summary = "获取当前用户在活动中可评价的成员")
    @GetMapping("/activities/{activityId}/reviews/targets")
    public Result<List<ReviewTargetVO>> targets(@PathVariable Long activityId) {
        return Result.ok(reviewService.targets(activityId));
    }

    @Operation(summary = "提交活动评价")
    @PostMapping("/activities/{activityId}/reviews")
    public Result<ActivityReviewVO> create(@PathVariable Long activityId,
                                           @Valid @RequestBody ReviewCreateDTO dto) {
        return Result.ok(reviewService.create(activityId, dto));
    }

    @Operation(summary = "分页获取活动评价")
    @GetMapping("/activities/{activityId}/reviews")
    public Result<PageResult<ActivityReviewVO>> activityReviews(@PathVariable Long activityId,
                                                                @RequestParam(defaultValue = "1") Long current,
                                                                @RequestParam(defaultValue = "20") Long size) {
        return Result.ok(reviewService.activityReviews(activityId, current, size));
    }

    @Operation(summary = "分页获取我发出或收到的评价")
    @GetMapping("/reviews/my")
    public Result<PageResult<ActivityReviewVO>> myReviews(@RequestParam String type,
                                                          @RequestParam(defaultValue = "1") Long current,
                                                          @RequestParam(defaultValue = "20") Long size) {
        return Result.ok(reviewService.myReviews(type, current, size));
    }
}
