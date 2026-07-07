package com.cityparty.module.favorite.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_favorite")
public class ActivityFavorite {

    private Long id;
    private Long userId;
    private Long activityId;
    private LocalDateTime createdAt;
    private Integer deleted;
}
