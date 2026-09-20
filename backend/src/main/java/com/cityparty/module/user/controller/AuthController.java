package com.cityparty.module.user.controller;

import com.cityparty.common.result.Result;
import com.cityparty.common.security.ClientIpResolver;
import com.cityparty.module.user.dto.LoginDTO;
import com.cityparty.module.user.dto.RegisterDTO;
import com.cityparty.module.user.dto.RegistrationCaptchaVerifyDTO;
import com.cityparty.module.user.service.AuthService;
import com.cityparty.module.user.service.RegistrationCaptchaService;
import com.cityparty.module.user.vo.CaptchaVO;
import com.cityparty.module.user.vo.LoginVO;
import com.cityparty.module.user.vo.RegistrationCaptchaChallengeVO;
import com.cityparty.module.user.vo.RegistrationCaptchaVerifyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "登录注册")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RegistrationCaptchaService registrationCaptchaService;
    private final ClientIpResolver clientIpResolver;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(authService.captcha());
    }

    @Operation(summary = "获取注册滑块挑战")
    @GetMapping("/register-captcha/challenge")
    public Result<RegistrationCaptchaChallengeVO> registrationCaptcha(HttpServletRequest request) {
        return Result.ok(registrationCaptchaService.createChallenge(clientIpResolver.resolve(request)));
    }

    @Operation(summary = "验证注册滑块挑战")
    @PostMapping("/register-captcha/verify")
    public Result<RegistrationCaptchaVerifyVO> verifyRegistrationCaptcha(
            @Valid @RequestBody RegistrationCaptchaVerifyDTO dto,
            HttpServletRequest request) {
        return Result.ok(registrationCaptchaService.verify(dto, clientIpResolver.resolve(request)));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        return Result.ok(authService.register(dto, clientIpResolver.resolve(request)));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }
}
