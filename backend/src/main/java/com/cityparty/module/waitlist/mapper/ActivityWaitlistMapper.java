package com.cityparty.module.waitlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityparty.module.waitlist.entity.ActivityWaitlist;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ActivityWaitlistMapper extends BaseMapper<ActivityWaitlist> {

    @Select("""
            SELECT COUNT(DISTINCT activity_id)
            FROM activity_waitlist
            WHERE user_id = #{userId}
              AND status = 'WAITING'
              AND deleted = 0
            """)
    Long countWaitingActivities(@Param("userId") Long userId);
}
