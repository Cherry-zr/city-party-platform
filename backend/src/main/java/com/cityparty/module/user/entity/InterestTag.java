package com.cityparty.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interest_tag")
public class InterestTag {

    private Long id;
    private String name;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
