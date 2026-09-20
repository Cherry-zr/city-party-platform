package com.cityparty.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivityAuditDTO {

    @NotBlank(message = "审核状态不能为空")
    private String auditStatus;

    @Size(max = 500, message = "拒绝原因不能超过 500 个字符")
    private String rejectReason;
}
