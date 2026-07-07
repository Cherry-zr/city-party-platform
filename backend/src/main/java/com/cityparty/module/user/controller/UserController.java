package com.cityparty.module.user.controller;

import com.cityparty.common.result.Result;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.user.dto.UpdateProfileDTO;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.PublicUserProfileVO;
import com.cityparty.module.user.vo.UserMeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户资料")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/user/me")
    public Result<UserMeVO> me() {
        return Result.ok(userService.getMe(UserContext.getUserId()));
    }

    @Operation(summary = "修改当前用户资料")
    @PutMapping("/user/profile")
    public Result<UserMeVO> updateProfile(@RequestBody UpdateProfileDTO dto) {
        return Result.ok(userService.updateProfile(UserContext.getUserId(), dto));
    }

    @Operation(summary = "查看用户公开主页")
    @GetMapping("/users/{id}/public-profile")
    public Result<PublicUserProfileVO> publicProfile(@PathVariable Long id) {
        return Result.ok(userService.publicProfile(id));
    }
}
