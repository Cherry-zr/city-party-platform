package com.cityparty.module.admin.vo;

import lombok.Data;

@Data
public class CreditUserVO {

    private Long userId;
    private String username;
    private String nickname;
    private String city;
    private Integer creditScore;
}
