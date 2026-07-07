package com.cityparty.module.signup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_signup")
public class ActivitySignup {

    private Long id;
    private Long activityId;
    private Long userId;
    private String status;
    private String applyMessage;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
