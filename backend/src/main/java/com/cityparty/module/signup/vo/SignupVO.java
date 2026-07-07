package com.cityparty.module.signup.vo;

import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.user.vo.UserMeVO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SignupVO {

    private Long id;
    private Long activityId;
    private Long userId;
    private String status;
    private String applyMessage;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private ActivityVO activity;
    private UserMeVO user;
}
