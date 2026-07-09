package com.cityparty.module.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_review")
public class ActivityReview {

    private Long id;
    private Long activityId;
    private Long reviewerId;
    private Long targetUserId;
    private Integer rating;
    private String content;
    private String tags;
    private Integer creditDelta;
    private LocalDateTime createdAt;
    private Integer deleted;
}
