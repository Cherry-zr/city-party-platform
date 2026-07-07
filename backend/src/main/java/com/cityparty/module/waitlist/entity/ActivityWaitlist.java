package com.cityparty.module.waitlist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_waitlist")
public class ActivityWaitlist {

    private Long id;
    private Long activityId;
    private Long userId;
    private String status;
    private Long queueNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
