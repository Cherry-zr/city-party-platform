package com.cityparty.module.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.favorite.entity.ActivityFavorite;
import com.cityparty.module.favorite.mapper.ActivityFavoriteMapper;
import com.cityparty.module.favorite.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final ActivityFavoriteMapper favoriteMapper;
    private final ActivityMapper activityMapper;
    private final ActivityService activityService;

    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long activityId) {
        Long userId = UserContext.getUserId();
        Activity activity = activityService.requireActivity(activityId);
        ActivityFavorite existed = favoriteMapper.selectOne(new LambdaQueryWrapper<ActivityFavorite>()
                .eq(ActivityFavorite::getActivityId, activityId)
                .eq(ActivityFavorite::getUserId, userId)
                .last("limit 1"));
        if (existed != null && Integer.valueOf(0).equals(existed.getDeleted())) {
            throw new BusinessException("已收藏该活动");
        }
        if (existed == null) {
            existed = new ActivityFavorite();
            existed.setActivityId(activityId);
            existed.setUserId(userId);
            existed.setCreatedAt(LocalDateTime.now());
            existed.setDeleted(0);
            favoriteMapper.insert(existed);
        } else {
            existed.setDeleted(0);
            favoriteMapper.updateById(existed);
        }
        activity.setFavoriteCount(activity.getFavoriteCount() + 1);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unfavorite(Long activityId) {
        Long userId = UserContext.getUserId();
        ActivityFavorite favorite = favoriteMapper.selectOne(new LambdaQueryWrapper<ActivityFavorite>()
                .eq(ActivityFavorite::getActivityId, activityId)
                .eq(ActivityFavorite::getUserId, userId)
                .eq(ActivityFavorite::getDeleted, 0)
                .last("limit 1"));
        if (favorite == null) {
            throw new BusinessException("尚未收藏该活动");
        }
        favorite.setDeleted(1);
        favoriteMapper.updateById(favorite);
        Activity activity = activityService.requireActivity(activityId);
        activity.setFavoriteCount(Math.max(0, activity.getFavoriteCount() - 1));
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
    }

    public PageResult<FavoriteVO> myFavorites(long current, long size) {
        Page<ActivityFavorite> page = favoriteMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<ActivityFavorite>()
                .eq(ActivityFavorite::getUserId, UserContext.getUserId())
                .eq(ActivityFavorite::getDeleted, 0)
                .orderByDesc(ActivityFavorite::getCreatedAt));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    private FavoriteVO toVO(ActivityFavorite favorite) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(favorite.getId());
        vo.setActivityId(favorite.getActivityId());
        vo.setCreatedAt(favorite.getCreatedAt());
        vo.setActivity(activityService.toVO(activityMapper.selectById(favorite.getActivityId())));
        return vo;
    }
}
