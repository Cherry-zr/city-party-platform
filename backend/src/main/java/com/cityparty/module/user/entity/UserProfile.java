package com.cityparty.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile {

    private Long id;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String city;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
