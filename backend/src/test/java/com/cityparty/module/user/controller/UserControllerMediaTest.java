package com.cityparty.module.user.controller;

import com.cityparty.common.config.UploadProperties;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.security.UserContext;
import com.cityparty.common.utils.FileUploadUtils;
import com.cityparty.module.user.dto.UpdateProfileDTO;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.UserMeVO;
import org.junit.jupiter.api.AfterEach;
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
class UserControllerMediaTest {

    @Mock
    private UserService userService;
    @Mock
    private FileUploadUtils fileUploadUtils;
    @Mock
    private UploadProperties uploadProperties;
    @InjectMocks
    private UserController controller;

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(2L, "user01", "USER"));
        lenient().when(uploadProperties.getAvatarDir()).thenReturn("avatar");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void rejectsUploadingAndRemovingAvatarTogether() {
        MockMultipartFile avatar = new MockMultipartFile("avatar", "avatar.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> controller.updateProfileWithAvatar(new UpdateProfileDTO(), avatar, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同时上传和删除");
    }

    @Test
    void deletesNewAvatarWhenProfileSaveFails() {
        UpdateProfileDTO dto = new UpdateProfileDTO();
        MockMultipartFile avatar = new MockMultipartFile("avatar", "avatar.jpg", "image/jpeg", new byte[]{1});
        String uploadedUrl = "/uploads/avatar/11111111-1111-1111-1111-111111111111.jpg";
        UserMeVO me = new UserMeVO();
        me.setAvatarUrl(null);
        when(userService.getMe(2L)).thenReturn(me);
        when(fileUploadUtils.uploadCroppedJpeg(avatar, "avatar", 512, 512)).thenReturn(uploadedUrl);
        when(userService.updateProfile(2L, dto)).thenThrow(new BusinessException("save failed"));

        assertThatThrownBy(() -> controller.updateProfileWithAvatar(dto, avatar, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("save failed");

        verify(fileUploadUtils).deleteManagedFile(uploadedUrl, "avatar");
    }
}
