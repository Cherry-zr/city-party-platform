package com.cityparty.module.credit.vo;

import com.cityparty.common.result.PageResult;
import lombok.Data;

@Data
public class CreditOverviewVO {

    private Integer creditScore;
    private String creditLevel;
    private PageResult<CreditRecordVO> records;
}
