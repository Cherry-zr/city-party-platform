package com.cityparty.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_interest")
public class UserInterest {

    private Long id;
    private Long userId;
    private Long tagId;
    private LocalDateTime createdAt;
}
