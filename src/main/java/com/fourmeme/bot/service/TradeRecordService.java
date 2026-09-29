package com.fourmeme.bot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fourmeme.bot.entity.TradeRecord;

import java.util.List;

public interface TradeRecordService {

    /**
     * 保存交易记录
     */
    void save(TradeRecord record);

    /**
     * 查询交易记录（按代币简称、钱包ID过滤，默认最近100条）
     */
    List<TradeRecord> listRecent(String tokenName, Long walletId);

    /**
     * 分页查询交易记录
     * @param tokenName 代币简称（可选，like 匹配）
     * @param walletId  钱包ID（可选）
     * @param pageNum   页码，从 1 开始
     * @param pageSize  每页条数
     */
    IPage<TradeRecord> listPage(String tokenName, Long walletId, int pageNum, int pageSize);
}