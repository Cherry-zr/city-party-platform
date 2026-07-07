package com.cityparty.module.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserMeVO {

    private Long id;
    private String username;
    private String phone;
    private String role;
    private String status;
    private Integer creditScore;
    private String nickname;
    private String avatarUrl;
    private String city;
    private String bio;
    private List<String> interestTags;
}
