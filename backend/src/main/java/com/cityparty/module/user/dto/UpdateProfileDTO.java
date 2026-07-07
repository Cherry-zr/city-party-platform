package com.cityparty.module.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileDTO {

    private String nickname;
    private String avatarUrl;
    private String city;
    private String bio;
    private List<String> interestTags;
}
