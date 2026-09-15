package com.cityparty.module.admin.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.activity.service.PublicActivityCacheService;
import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.admin.dto.ActivityAuditDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ActivityAuditService {

    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final PublicActivityCacheService publicActivityCacheService;

    @Transactional(rollbackFor = Exception.class)
    public ActivityVO audit(Long activityId, ActivityAuditDTO dto) {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "只有管理员可以审核活动");
        }
        String auditStatus = dto.getAuditStatus().trim().toUpperCase(Locale.ROOT);
        if (!"APPROVED".equals(auditStatus) && !"REJECTED".equals(auditStatus)) {
            throw new BusinessException(400, "审核状态只能是 APPROVED 或 REJECTED");
        }
        if ("REJECTED".equals(auditStatus) && !StringUtils.hasText(dto.getRejectReason())) {
            throw new BusinessException(400, "拒绝活动时必须填写原因");
        }

        Activity activity = activityService.requireActivity(activityId);
        activity.setAuditStatus(auditStatus);
        activity.setRejectReason("REJECTED".equals(auditStatus) ? dto.getRejectReason().trim() : null);
        activity.setAuditTime(LocalDateTime.now());
        activity.setReviewerId(UserContext.getUserId());
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        publicActivityCacheService.evictRecommendationCachesAfterCommit();
        return activityService.toVO(activity);
    }
}
