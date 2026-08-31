package com.cityparty.module.recommendation.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecommendationScoreDetailVO {

    private BigDecimal interest;
    private BigDecimal distance;
    private BigDecimal hotness;
    private BigDecimal time;
    private BigDecimal credit;
}
