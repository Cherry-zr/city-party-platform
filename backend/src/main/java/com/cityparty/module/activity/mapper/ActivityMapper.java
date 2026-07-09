package com.cityparty.module.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.module.activity.entity.Activity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface ActivityMapper extends BaseMapper<Activity> {

    Page<Activity> selectMyActivities(Page<Activity> page,
                                      @Param("userId") Long userId,
                                      @Param("type") String type,
                                      @Param("now") LocalDateTime now);
}
