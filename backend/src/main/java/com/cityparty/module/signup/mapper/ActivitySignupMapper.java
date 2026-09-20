package com.cityparty.module.signup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.module.signup.entity.ActivitySignup;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ActivitySignupMapper extends BaseMapper<ActivitySignup> {

    @Select("""
            SELECT COUNT(DISTINCT s.activity_id)
            FROM activity_signup s
            INNER JOIN activity a ON a.id = s.activity_id
            WHERE s.user_id = #{userId}
              AND s.status IN ('APPROVED', 'PROMOTED', 'COMPLETED')
              AND s.deleted = 0
              AND a.deleted = 0
              AND a.audit_status = 'APPROVED'
            """)
    Long countJoinedActivities(@Param("userId") Long userId);

    @Select("""
            SELECT s.*
            FROM activity_signup s
            INNER JOIN activity a ON a.id = s.activity_id
            WHERE s.user_id = #{userId}
              AND s.deleted = 0
              AND a.deleted = 0
              AND a.audit_status = 'APPROVED'
            ORDER BY s.created_at DESC, s.id DESC
            """)
    Page<ActivitySignup> selectApprovedPage(Page<ActivitySignup> page, @Param("userId") Long userId);
}
