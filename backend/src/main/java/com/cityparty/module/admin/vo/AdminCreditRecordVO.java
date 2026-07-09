package com.cityparty.module.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCreditRecordVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private Integer changeScore;
    private Integer beforeScore;
    private Integer afterScore;
    private String reason;
    private String sourceType;
    private Long sourceId;
    private Long activityId;
    private String activityTitle;
    private LocalDateTime createdAt;
}
