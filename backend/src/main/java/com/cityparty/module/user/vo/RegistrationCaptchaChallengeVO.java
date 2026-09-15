package com.cityparty.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationCaptchaChallengeVO {

    private String challengeId;
    private String backgroundImage;
    private String sliderImage;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer sliderWidth;
    private Integer sliderHeight;
    private Integer sliderY;
    private Long expiresInSeconds;
}
