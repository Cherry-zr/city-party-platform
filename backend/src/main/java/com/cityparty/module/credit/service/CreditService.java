package com.cityparty.module.credit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.common.utils.PageUtils;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.credit.CreditLevelResolver;
import com.cityparty.module.credit.entity.CreditRecord;
import com.cityparty.module.credit.mapper.CreditRecordMapper;
import com.cityparty.module.credit.vo.CreditOverviewVO;
import com.cityparty.module.credit.vo.CreditRecordVO;
import com.cityparty.module.review.entity.ActivityReview;
import com.cityparty.module.review.mapper.ActivityReviewMapper;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditService {

    private static final Set<String> REVIEW_SOURCE_TYPES = Set.of("ACTIVITY_REVIEW", "REVIEW");

    private final CreditRecordMapper creditRecordMapper;
    private final ActivityReviewMapper reviewMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;

    public CreditOverviewVO overview(long current, long size) {
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        CreditOverviewVO vo = new CreditOverviewVO();
        vo.setCreditScore(user.getCreditScore());
        vo.setCreditLevel(CreditLevelResolver.resolve(user.getCreditScore()));
        vo.setRecords(myLogs(current, size));
        return vo;
    }

    public PageResult<CreditRecordVO> myLogs(long current, long size) {
        Page<CreditRecord> page = creditRecordMapper.selectPage(
                PageUtils.page(current, size),
                new LambdaQueryWrapper<CreditRecord>()
                        .eq(CreditRecord::getUserId, UserContext.getUserId())
                        .eq(CreditRecord::getDeleted, 0)
                        .orderByDesc(CreditRecord::getCreatedAt)
        );
        List<CreditRecord> records = page.getRecords();
        Map<Long, ActivityReview> reviews = loadRelatedReviews(records);
        Map<Long, Activity> activities = loadRelatedActivities(reviews.values());
        return new PageResult<>(
                records.stream().map(record -> toVO(record, reviews, activities)).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private CreditRecordVO toVO(CreditRecord record,
                                Map<Long, ActivityReview> reviews,
                                Map<Long, Activity> activities) {
        CreditRecordVO vo = new CreditRecordVO();
        vo.setId(record.getId());
        vo.setChangeValue(record.getChangeScore());
        vo.setBeforeScore(record.getBeforeScore());
        vo.setAfterScore(record.getAfterScore());
        vo.setReason(record.getReason());
        vo.setSourceType(record.getSourceType());
        vo.setSourceId(record.getSourceId());
        ActivityReview review = reviews.get(record.getSourceId());
        if (review != null) {
            Activity activity = activities.get(review.getActivityId());
            if (activity != null) {
                vo.setRelatedActivityId(activity.getId());
                vo.setRelatedActivityTitle(activity.getTitle());
            }
        }
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private Map<Long, ActivityReview> loadRelatedReviews(List<CreditRecord> records) {
        List<Long> reviewIds = records.stream()
                .filter(this::isReviewSource)
                .map(CreditRecord::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (reviewIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return reviewMapper.selectBatchIds(reviewIds).stream()
                .collect(Collectors.toMap(ActivityReview::getId, Function.identity()));
    }

    private Map<Long, Activity> loadRelatedActivities(Iterable<ActivityReview> reviews) {
        List<Long> activityIds = new java.util.ArrayList<>();
        reviews.forEach(review -> {
            if (review.getActivityId() != null && !activityIds.contains(review.getActivityId())) {
                activityIds.add(review.getActivityId());
            }
        });
        if (activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return activityMapper.selectBatchIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, Function.identity()));
    }

    private boolean isReviewSource(CreditRecord record) {
        return record.getSourceType() != null
                && REVIEW_SOURCE_TYPES.contains(record.getSourceType().toUpperCase(Locale.ROOT));
    }
}
