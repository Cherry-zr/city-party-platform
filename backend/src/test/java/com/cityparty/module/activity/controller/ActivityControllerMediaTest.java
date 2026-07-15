package com.cityparty.module.activity.controller;

import com.cityparty.common.config.UploadProperties;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.utils.FileUploadUtils;
import com.cityparty.module.activity.dto.ActivityCreateDTO;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.signup.service.SignupService;
import com.cityparty.module.waitlist.service.ActivityWaitlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityControllerMediaTest {

    @Mock
    private ActivityService activityService;
    @Mock
    private SignupService signupService;
    @Mock
    private ActivityWaitlistService waitlistService;
    @Mock
    private FileUploadUtils fileUploadUtils;
    @Mock
    private UploadProperties uploadProperties;
    @InjectMocks
    private ActivityController controller;

    @BeforeEach
    void setUp() {
        lenient().when(uploadProperties.getActivityDir()).thenReturn("activity");
    }

    @Test
    void rejectsUploadingAndRemovingCoverTogether() {
        MockMultipartFile cover = new MockMultipartFile("cover", "cover.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> controller.updateWithCover(1L, new ActivityCreateDTO(), cover, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同时上传和删除");
    }

    @Test
    void deletesNewCoverWhenCreateFails() {
        ActivityCreateDTO dto = new ActivityCreateDTO();
        MockMultipartFile cover = new MockMultipartFile("cover", "cover.jpg", "image/jpeg", new byte[]{1});
        String uploadedUrl = "/uploads/activity/11111111-1111-1111-1111-111111111111.jpg";
        when(fileUploadUtils.uploadCroppedJpeg(cover, "activity", 1200, 500)).thenReturn(uploadedUrl);
        when(activityService.create(dto)).thenThrow(new BusinessException("save failed"));

        assertThatThrownBy(() -> controller.createWithCover(dto, cover))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("save failed");

        verify(fileUploadUtils).deleteManagedFile(uploadedUrl, "activity");
    }

    @Test
    void deletesPreviousCoverAfterSuccessfulReplacement() {
        ActivityCreateDTO dto = new ActivityCreateDTO();
        MockMultipartFile cover = new MockMultipartFile("cover", "cover.jpg", "image/jpeg", new byte[]{1});
        String previousUrl = "/uploads/activity/11111111-1111-1111-1111-111111111111.jpg";
        String uploadedUrl = "/uploads/activity/22222222-2222-2222-2222-222222222222.jpg";
        Activity current = new Activity();
        current.setCoverUrl(previousUrl);
        ActivityVO updated = new ActivityVO();
        updated.setCoverUrl(uploadedUrl);
        when(activityService.validateUpdateRequest(1L, dto)).thenReturn(current);
        when(fileUploadUtils.uploadCroppedJpeg(cover, "activity", 1200, 500)).thenReturn(uploadedUrl);
        when(activityService.update(1L, dto)).thenReturn(updated);

        controller.updateWithCover(1L, dto, cover, false);

        verify(fileUploadUtils).deleteManagedFile(previousUrl, "activity");
    }
}
