package com.fourmeme.bot.service;

import com.fourmeme.bot.entity.TokenMeta;

public interface TokenMetaService {

    /**
     * 获取代币元数据
     * 优先从数据库读取，不存在则从链上获取并写入数据库
     *
     * @param tokenAddress 代币合约地址
     * @return 元数据实体，链上也获取失败时返回 null
     */
    TokenMeta getOrFetch(String tokenAddress);

    /**
     * 手动保存或更新代币元数据（source=MANUAL）
     * 用于人工修正链上获取失败或错误的数据
     */
    TokenMeta saveManual(TokenMeta metadata);

    /**
     * 根据地址查询（不触发链上拉取）
     */
    TokenMeta getByAddress(String tokenAddress);
}