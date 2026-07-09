package com.cityparty.module.signup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityparty.module.signup.entity.ActivitySignup;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ActivitySignupMapper extends BaseMapper<ActivitySignup> {

    @Select("""
            SELECT COUNT(DISTINCT activity_id)
            FROM activity_signup
            WHERE user_id = #{userId}
              AND status IN ('APPROVED', 'PROMOTED', 'COMPLETED')
              AND deleted = 0
            """)
    Long countJoinedActivities(@Param("userId") Long userId);
}
