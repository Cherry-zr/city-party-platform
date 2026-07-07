package com.cityparty.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String phone;
    private String nickname;
    private String city;

    @NotBlank(message = "验证码 key 不能为空")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
