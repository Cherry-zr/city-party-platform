package com.cityparty.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {

    private String captchaKey;
    private String captchaText;
}
