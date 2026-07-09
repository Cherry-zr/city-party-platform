package com.cityparty.module.credit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.credit.entity.CreditRecord;
import com.cityparty.module.credit.mapper.CreditRecordMapper;
import com.cityparty.module.credit.vo.CreditRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRecordMapper creditRecordMapper;

    public PageResult<CreditRecordVO> myLogs(long current, long size) {
        long safeCurrent = Math.max(current, 1);
        long safeSize = Math.min(Math.max(size, 1), 100);
        Page<CreditRecord> page = creditRecordMapper.selectPage(
                new Page<>(safeCurrent, safeSize),
                new LambdaQueryWrapper<CreditRecord>()
                        .eq(CreditRecord::getUserId, UserContext.getUserId())
                        .eq(CreditRecord::getDeleted, 0)
                        .orderByDesc(CreditRecord::getCreatedAt)
        );
        return new PageResult<>(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private CreditRecordVO toVO(CreditRecord record) {
        CreditRecordVO vo = new CreditRecordVO();
        vo.setId(record.getId());
        vo.setChangeValue(record.getChangeScore());
        vo.setBeforeScore(record.getBeforeScore());
        vo.setAfterScore(record.getAfterScore());
        vo.setReason(record.getReason());
        vo.setSourceType(record.getSourceType());
        vo.setSourceId(record.getSourceId());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
