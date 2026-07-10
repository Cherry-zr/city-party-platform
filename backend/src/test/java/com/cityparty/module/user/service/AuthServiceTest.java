package com.cityparty.module.user.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.JwtUtils;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.utils.PasswordUtils;
import com.cityparty.module.user.dto.LoginDTO;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import com.cityparty.module.user.vo.LoginVO;
import com.cityparty.module.user.vo.UserMeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CAPTCHA_KEY = "test-captcha-key";
    private static final String CAPTCHA_CODE = "ABCD";
    private static final String RAW_PASSWORD = "test-password";
    private static final String LEGACY_HASH = "a".repeat(64);
    private static final String PBKDF2_HASH = "pbkdf2$120000$00112233445566778899aabbccddeeff$hash";

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private PasswordUtils passwordUtils;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha:" + CAPTCHA_KEY)).thenReturn(CAPTCHA_CODE);
    }

    @Test
    void upgradesLegacyHashAfterSuccessfulLogin() {
        User user = normalUser(LEGACY_HASH);
        LoginDTO dto = loginDTO();
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordUtils.matches(RAW_PASSWORD, LEGACY_HASH)).thenReturn(true);
        when(passwordUtils.needsUpgrade(LEGACY_HASH)).thenReturn(true);
        when(passwordUtils.encode(RAW_PASSWORD)).thenReturn(PBKDF2_HASH);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        prepareLoginResponse(user);

        LoginVO result = authService.login(dto);

        assertThat(result.getToken()).isEqualTo("test-token");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo(PBKDF2_HASH);
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void doesNotRewritePbkdf2HashAfterSuccessfulLogin() {
        User user = normalUser(PBKDF2_HASH);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordUtils.matches(RAW_PASSWORD, PBKDF2_HASH)).thenReturn(true);
        when(passwordUtils.needsUpgrade(PBKDF2_HASH)).thenReturn(false);
        prepareLoginResponse(user);

        LoginVO result = authService.login(loginDTO());

        assertThat(result.getToken()).isEqualTo("test-token");
        verify(passwordUtils, never()).encode(any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void doesNotUpgradeHashWhenPasswordIsInvalid() {
        User user = normalUser(LEGACY_HASH);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordUtils.matches(RAW_PASSWORD, LEGACY_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号或密码错误");

        verify(passwordUtils, never()).needsUpgrade(any());
        verify(passwordUtils, never()).encode(any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void doesNotUpgradeHashForDisabledUser() {
        User user = normalUser(LEGACY_HASH);
        user.setStatus("DISABLED");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordUtils.matches(RAW_PASSWORD, LEGACY_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已被禁用");

        verify(passwordUtils, never()).needsUpgrade(any());
        verify(passwordUtils, never()).encode(any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    private void prepareLoginResponse(User user) {
        when(jwtUtils.generateToken(any(LoginUser.class))).thenReturn("test-token");
        when(userService.getMe(user.getId())).thenReturn(new UserMeVO());
    }

    private LoginDTO loginDTO() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("demo-user");
        dto.setPassword(RAW_PASSWORD);
        dto.setCaptchaKey(CAPTCHA_KEY);
        dto.setCaptchaCode(CAPTCHA_CODE);
        return dto;
    }

    private User normalUser(String passwordHash) {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo-user");
        user.setPasswordHash(passwordHash);
        user.setRole("USER");
        user.setStatus("NORMAL");
        user.setDeleted(0);
        return user;
    }
}
