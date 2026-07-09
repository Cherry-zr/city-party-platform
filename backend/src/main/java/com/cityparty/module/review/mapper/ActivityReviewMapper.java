package com.cityparty.module.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityparty.module.review.entity.ActivityReview;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

public interface ActivityReviewMapper extends BaseMapper<ActivityReview> {

    @Select("""
            SELECT AVG(rating)
            FROM activity_review
            WHERE target_user_id = #{userId}
              AND deleted = 0
            """)
    BigDecimal selectAverageRatingByTargetUserId(@Param("userId") Long userId);
}
