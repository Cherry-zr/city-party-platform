package com.cityparty.module.credit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.credit.entity.CreditRecord;
import com.cityparty.module.credit.mapper.CreditRecordMapper;
import com.cityparty.module.review.entity.ActivityReview;
import com.cityparty.module.review.mapper.ActivityReviewMapper;
import com.cityparty.module.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class CreditServiceTest {

    @Mock
    private CreditRecordMapper creditRecordMapper;
    @Mock
    private ActivityReviewMapper reviewMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private CreditService creditService;

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(2L, "user01", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void creditLogsResolveRelatedActivityFromReviewSource() {
        CreditRecord record = new CreditRecord();
        record.setId(1L);
        record.setSourceType("ACTIVITY_REVIEW");
        record.setSourceId(10L);
        record.setChangeScore(2);
        record.setBeforeScore(100);
        record.setAfterScore(102);

        ActivityReview review = new ActivityReview();
        review.setId(10L);
        review.setActivityId(20L);
        Activity activity = new Activity();
        activity.setId(20L);
        activity.setTitle("Stage 2.4 验收活动");

        Page<CreditRecord> page = new Page<>(1, 20);
        page.setRecords(List.of(record));
        page.setTotal(1);
        when(creditRecordMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(reviewMapper.selectBatchIds(any())).thenReturn(List.of(review));
        when(activityMapper.selectBatchIds(any())).thenReturn(List.of(activity));

        var result = creditService.myLogs(1, 20);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getRelatedActivityId()).isEqualTo(20L);
        assertThat(result.getRecords().get(0).getRelatedActivityTitle()).isEqualTo("Stage 2.4 验收活动");
    }
}
