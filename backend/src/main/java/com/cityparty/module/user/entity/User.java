package com.cityparty.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    private Long id;
    private String username;
    private String phone;
    private String passwordHash;
    private String role;
    private String status;
    private Integer creditScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
