package com.cityparty.module.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReviewCreateDTO {

    @NotNull(message = "请选择评价对象")
    private Long targetUserId;

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分必须在 1 到 5 之间")
    @Max(value = 5, message = "评分必须在 1 到 5 之间")
    private Integer rating;

    @Size(max = 500, message = "评价内容不能超过 500 字")
    private String content;

    @Size(max = 5, message = "评价标签不能超过 5 个")
    private List<String> tags;
}
