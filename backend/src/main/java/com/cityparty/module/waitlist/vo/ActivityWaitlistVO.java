package com.cityparty.module.waitlist.vo;

import com.cityparty.module.user.vo.UserMeVO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityWaitlistVO {

    private Long id;
    private Long activityId;
    private String activityTitle;
    private Long userId;
    private String status;
    private Long queueNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserMeVO user;
}
