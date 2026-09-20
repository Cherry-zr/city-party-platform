package com.cityparty.module.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.module.favorite.entity.ActivityFavorite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ActivityFavoriteMapper extends BaseMapper<ActivityFavorite> {

    @Select("""
            SELECT f.*
            FROM activity_favorite f
            INNER JOIN activity a ON a.id = f.activity_id
            WHERE f.user_id = #{userId}
              AND f.deleted = 0
              AND a.deleted = 0
              AND a.audit_status = 'APPROVED'
            ORDER BY f.created_at DESC, f.id DESC
            """)
    Page<ActivityFavorite> selectApprovedPage(Page<ActivityFavorite> page, @Param("userId") Long userId);
}
