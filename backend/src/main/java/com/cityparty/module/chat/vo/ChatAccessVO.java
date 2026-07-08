package com.cityparty.module.chat.vo;

import lombok.Data;

@Data
public class ChatAccessVO {

    private Boolean canAccess;
    private String reason;
    private Long activityId;
    private String activityTitle;
}
