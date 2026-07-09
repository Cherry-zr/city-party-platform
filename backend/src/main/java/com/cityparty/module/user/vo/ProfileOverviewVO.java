package com.cityparty.module.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProfileOverviewVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String city;
    private String bio;
    private List<String> interestTags;
    private Integer creditScore;
    private String creditLevel;
    private Long publishedActivityCount;
    private Long joinedActivityCount;
    private Long waitingActivityCount;
    private Long receivedReviewCount;
    private BigDecimal averageRating;
    private Long unreadNoticeCount;
}
