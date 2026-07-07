package com.cityparty.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.JwtUtils;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.utils.PasswordUtils;
import com.cityparty.module.user.dto.LoginDTO;
import com.cityparty.module.user.dto.RegisterDTO;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.entity.UserProfile;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import com.cityparty.module.user.vo.CaptchaVO;
import com.cityparty.module.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordUtils passwordUtils;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    public CaptchaVO captcha() {
        String key = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode();
        stringRedisTemplate.opsForValue().set(CAPTCHA_PREFIX + key, code, Duration.ofMinutes(5));
        return new CaptchaVO(key, code);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO dto) {
        verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        User existed = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())
                .last("limit 1"));
        if (existed != null) {
            throw new BusinessException("账号已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setPasswordHash(passwordUtils.encode(dto.getPassword()));
        user.setRole("USER");
        user.setStatus("NORMAL");
        user.setCreditScore(100);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        userMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        profile.setAvatarUrl(null);
        profile.setCity(StringUtils.hasText(dto.getCity()) ? dto.getCity() : "北京");
        profile.setBio("这个人正在寻找有趣的同城活动。");
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profile.setDeleted(0);
        userProfileMapper.insert(profile);

        return buildLoginVO(user);
    }

    public LoginVO login(LoginDTO dto) {
        verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())
                .last("limit 1"));
        if (user == null || !passwordUtils.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        if (!"NORMAL".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        return buildLoginVO(user);
    }

    private LoginVO buildLoginVO(User user) {
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole());
        String token = jwtUtils.generateToken(loginUser);
        return new LoginVO(token, userService.getMe(user.getId()));
    }

    private void verifyCaptcha(String key, String code) {
        String redisKey = CAPTCHA_PREFIX + key;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(storedCode)) {
            throw new BusinessException("验证码已过期");
        }
        if (!storedCode.equalsIgnoreCase(code)) {
            throw new BusinessException("验证码错误");
        }
        stringRedisTemplate.delete(redisKey);
    }

    private String randomCode() {
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            builder.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return builder.toString();
    }
}
