package com.cityparty.module.credit.controller;

import com.cityparty.common.result.PageResult;
import com.cityparty.common.result.Result;
import com.cityparty.module.credit.service.CreditService;
import com.cityparty.module.credit.vo.CreditOverviewVO;
import com.cityparty.module.credit.vo.CreditRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "信用分")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/credit")
public class CreditController {

    private final CreditService creditService;

    @Operation(summary = "获取当前用户信用概览和变更记录")
    @GetMapping("/overview")
    public Result<CreditOverviewVO> overview(@RequestParam(defaultValue = "1") Long current,
                                             @RequestParam(defaultValue = "20") Long size) {
        return Result.ok(creditService.overview(current, size));
    }

    @Operation(summary = "分页获取当前用户信用分变更记录")
    @GetMapping("/logs")
    public Result<PageResult<CreditRecordVO>> logs(@RequestParam(defaultValue = "1") Long current,
                                                   @RequestParam(defaultValue = "20") Long size) {
        return Result.ok(creditService.myLogs(current, size));
    }
}
