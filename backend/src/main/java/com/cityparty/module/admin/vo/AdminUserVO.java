package com.cityparty.module.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserVO {

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
    private LocalDateTime createdAt;
}
