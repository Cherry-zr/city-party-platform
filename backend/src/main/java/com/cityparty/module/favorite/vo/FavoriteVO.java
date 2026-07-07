package com.cityparty.module.favorite.vo;

import com.cityparty.module.activity.vo.ActivityVO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteVO {

    private Long id;
    private Long activityId;
    private LocalDateTime createdAt;
    private ActivityVO activity;
}
