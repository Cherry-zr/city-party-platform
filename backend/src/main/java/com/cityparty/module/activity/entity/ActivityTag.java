package com.cityparty.module.activity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_tag")
public class ActivityTag {

    private Long id;
    private Long activityId;
    private String tagName;
    private LocalDateTime createdAt;
}
