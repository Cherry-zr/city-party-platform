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
    private Long reviewedUserId;
    private Integer punctualityScore;
    private Integer communicationScore;
    private Integer authenticityScore;
    private Integer overallScore;
    private String content;
    private LocalDateTime createdAt;
    private Integer deleted;
}
