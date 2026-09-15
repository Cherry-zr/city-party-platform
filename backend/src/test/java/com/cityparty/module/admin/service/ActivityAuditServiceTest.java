package com.cityparty.module.admin.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.activity.service.PublicActivityCacheService;
import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.admin.dto.ActivityAuditDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAuditServiceTest {

    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivityService activityService;
    @Mock
    private PublicActivityCacheService publicActivityCacheService;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void adminCanRejectActivityAndReasonIsRecorded() {
        UserContext.set(new LoginUser(9L, "admin", "ADMIN"));
        Activity activity = pendingActivity();
        ActivityVO expected = new ActivityVO();
        expected.setId(activity.getId());
        expected.setAuditStatus("REJECTED");
        when(activityService.requireActivity(activity.getId())).thenReturn(activity);
        when(activityService.toVO(activity)).thenReturn(expected);
        ActivityAuditService service = service();

        ActivityAuditDTO dto = new ActivityAuditDTO();
        dto.setAuditStatus("rejected");
        dto.setRejectReason("地址描述不完整");
        ActivityVO result = service.audit(activity.getId(), dto);

        assertThat(result).isSameAs(expected);
        assertThat(activity.getAuditStatus()).isEqualTo("REJECTED");
        assertThat(activity.getRejectReason()).isEqualTo("地址描述不完整");
        assertThat(activity.getAuditTime()).isNotNull();
        assertThat(activity.getReviewerId()).isEqualTo(9L);
        verify(activityMapper).updateById(activity);
        verify(publicActivityCacheService).evictRecommendationCachesAfterCommit();
    }

    @Test
    void adminApprovalClearsOldRejectReason() {
        UserContext.set(new LoginUser(9L, "admin", "ADMIN"));
        Activity activity = pendingActivity();
        activity.setRejectReason("旧原因");
        when(activityService.requireActivity(activity.getId())).thenReturn(activity);
        when(activityService.toVO(activity)).thenReturn(new ActivityVO());
        ActivityAuditService service = service();

        ActivityAuditDTO dto = new ActivityAuditDTO();
        dto.setAuditStatus("APPROVED");
        service.audit(activity.getId(), dto);

        assertThat(activity.getAuditStatus()).isEqualTo("APPROVED");
        assertThat(activity.getRejectReason()).isNull();
    }

    @Test
    void normalUserCannotCallAuditService() {
        UserContext.set(new LoginUser(2L, "user", "USER"));
        ActivityAuditDTO dto = new ActivityAuditDTO();
        dto.setAuditStatus("APPROVED");

        assertThatThrownBy(() -> service().audit(10L, dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);

        verify(activityMapper, never()).updateById(org.mockito.ArgumentMatchers.any(Activity.class));
    }

    private ActivityAuditService service() {
        return new ActivityAuditService(activityMapper, activityService, publicActivityCacheService);
    }

    private Activity pendingActivity() {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setCreatorId(2L);
        activity.setAuditStatus("PENDING");
        activity.setDeleted(0);
        return activity;
    }
}
