package com.cityparty.module.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.module.activity.entity.Activity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface ActivityMapper extends BaseMapper<Activity> {

    Page<Activity> selectMyActivities(Page<Activity> page,
                                      @Param("userId") Long userId,
                                      @Param("type") String type,
                                      @Param("now") LocalDateTime now);

    @Update("""
            UPDATE activity
            SET status = CASE
                    WHEN approved_count + 1 >= max_participants THEN 'FULL'
                    WHEN status = 'FULL' THEN 'SIGNING'
                    ELSE status
                END,
                approved_count = approved_count + 1,
                updated_at = #{now}
            WHERE id = #{activityId}
              AND deleted = 0
              AND status IN ('SIGNING', 'FULL')
              AND approved_count < max_participants
            """)
    int increaseApprovedCountIfAvailable(@Param("activityId") Long activityId, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE activity
            SET status = CASE
                    WHEN approved_count - 1 < max_participants AND status = 'FULL' THEN 'SIGNING'
                    ELSE status
                END,
                approved_count = CASE WHEN approved_count > 0 THEN approved_count - 1 ELSE 0 END,
                updated_at = #{now}
            WHERE id = #{activityId}
              AND deleted = 0
              AND approved_count > 0
            """)
    int decreaseApprovedCount(@Param("activityId") Long activityId, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE activity
            SET status = CASE
                    WHEN approved_count >= max_participants THEN 'FULL'
                    ELSE 'SIGNING'
                END,
                updated_at = #{now}
            WHERE id = #{activityId}
              AND deleted = 0
              AND status IN ('SIGNING', 'FULL')
            """)
    int refreshJoinableStatus(@Param("activityId") Long activityId, @Param("now") LocalDateTime now);
}
