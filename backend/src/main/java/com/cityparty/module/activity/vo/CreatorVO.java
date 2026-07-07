package com.cityparty.module.activity.vo;

import lombok.Data;

@Data
public class CreatorVO {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private String city;
    private Integer creditScore;
}
