package com.cityparty.module.user.vo;

import com.cityparty.module.activity.vo.ActivityVO;
import lombok.Data;

import java.util.List;

@Data
public class PublicUserProfileVO {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private String city;
    private String bio;
    private Integer creditScore;
    private List<String> interestTags;
    private Long createdActivityCount;
    private Long joinedActivityCount;
    private List<ActivityVO> publicActivities;
}
