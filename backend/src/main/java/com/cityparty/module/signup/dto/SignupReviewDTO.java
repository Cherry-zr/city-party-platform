package com.cityparty.module.signup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupReviewDTO {

    @NotBlank(message = "审核状态不能为空")
    private String status;
}
