package com.cityparty.module.admin.vo;

import lombok.Data;

@Data
public class DashboardVO {

    private Long userCount;
    private Long activityCount;
    private Long signupCount;
    private Long favoriteCount;
    private Long waitlistCount;
    private Long unreadNoticeCount;
}
