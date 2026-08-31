package com.cityparty.module.recommendation.controller;

import com.cityparty.common.result.Result;
import com.cityparty.module.recommendation.service.RecommendationService;
import com.cityparty.module.recommendation.vo.RecommendedActivityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "个性化推荐")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "当前用户的个性化活动推荐")
    @GetMapping("/activities")
    public Result<List<RecommendedActivityVO>> activities(
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(defaultValue = "6") int limit) {
        return Result.ok(recommendationService.recommendActivities(longitude, latitude, limit));
    }
}
