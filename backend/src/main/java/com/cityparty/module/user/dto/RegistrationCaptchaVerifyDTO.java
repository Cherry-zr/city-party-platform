package com.cityparty.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RegistrationCaptchaVerifyDTO {

    @NotBlank(message = "滑块挑战 ID 不能为空")
    private String challengeId;

    @NotNull(message = "滑块位置不能为空")
    private Integer offsetX;

    @NotNull(message = "滑动时长不能为空")
    private Long durationMs;

    @Size(min = 3, max = 200, message = "滑动轨迹数据不合法")
    private List<Integer> trace;
}
