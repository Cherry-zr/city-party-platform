package com.cityparty.module.review.vo;

import lombok.Data;

@Data
public class ReviewTargetVO {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer creditScore;
    private Boolean reviewed;
}
