package com.cityparty.module.review.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityReviewVO {

    private Long id;
    private Long activityId;
    private String activityTitle;
    private Long reviewerId;
    private String reviewerNickname;
    private String reviewerAvatarUrl;
    private Long targetUserId;
    private String targetNickname;
    private String targetAvatarUrl;
    private Integer rating;
    private String content;
    private List<String> tags;
    private Integer creditDelta;
    private LocalDateTime createdAt;
}
