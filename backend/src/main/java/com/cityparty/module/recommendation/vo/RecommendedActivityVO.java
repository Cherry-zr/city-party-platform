package com.cityparty.module.recommendation.vo;

import com.cityparty.module.activity.vo.ActivityVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RecommendedActivityVO {

    private ActivityVO activity;
    private BigDecimal recommendationScore;
    private BigDecimal distanceKm;
    private List<String> reasons;
    private RecommendationScoreDetailVO scoreDetail;
}
