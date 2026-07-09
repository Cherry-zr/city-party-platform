package com.cityparty.module.admin.vo;

import lombok.Data;

@Data
public class DashboardVO {

    private Long userCount;
    private Long activityCount;
    private Long signupCount;
    private Long reviewCount;
    private Long noticeCount;
}
