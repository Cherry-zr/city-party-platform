package com.cityparty.module.favorite.controller;

import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.module.favorite.service.FavoriteService;
import com.cityparty.module.favorite.vo.FavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "活动收藏")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏活动")
    @PostMapping("/activities/{activityId}/favorite")
    public Result<Void> favorite(@PathVariable Long activityId) {
        favoriteService.favorite(activityId);
        return Result.ok();
    }

    @Operation(summary = "收藏活动（兼容接口）")
    @PostMapping("/favorites/{activityId}")
    public Result<Void> favoriteCompat(@PathVariable Long activityId) {
        favoriteService.favorite(activityId);
        return Result.ok();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/activities/{activityId}/favorite")
    public Result<Void> unfavorite(@PathVariable Long activityId) {
        favoriteService.unfavorite(activityId);
        return Result.ok();
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/favorites/my")
    public Result<PageResult<FavoriteVO>> myFavorites(@RequestParam(defaultValue = "1") Long current,
                                                      @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(favoriteService.myFavorites(current, size));
    }
}
