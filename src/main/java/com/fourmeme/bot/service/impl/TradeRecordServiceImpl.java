package com.fourmeme.bot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fourmeme.bot.entity.TradeRecord;
import com.fourmeme.bot.mapper.TradeRecordMapper;
import com.fourmeme.bot.service.TradeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeRecordServiceImpl implements TradeRecordService {

    private final TradeRecordMapper tradeRecordMapper;

    @Override
    public void save(TradeRecord record) {
        tradeRecordMapper.insert(record);
    }

    @Override
    public List<TradeRecord> listRecent(String tokenName, Long walletId) {
        LambdaQueryWrapper<TradeRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(tokenName)) {
            wrapper.eq(TradeRecord::getTokenName, tokenName);
        }
        if (walletId != null) {
            wrapper.eq(TradeRecord::getWalletId, walletId);
        }
        wrapper.orderByDesc(TradeRecord::getCreatedAt).last("LIMIT 100");
        return tradeRecordMapper.selectList(wrapper);
    }

    @Override
    public IPage<TradeRecord> listPage(String tokenName, Long walletId, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 200) pageSize = 20;

        Page<TradeRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TradeRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(tokenName)) {
            wrapper.like(TradeRecord::getTokenName, tokenName);
        }
        if (walletId != null) {
            wrapper.eq(TradeRecord::getWalletId, walletId);
        }
        wrapper.orderByDesc(TradeRecord::getCreatedAt);
        return tradeRecordMapper.selectPage(page, wrapper);
    }
}