package com.fourmeme.bot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fourmeme.bot.entity.TokenMeta;
import com.fourmeme.bot.enums.MarketPair;
import com.fourmeme.bot.mapper.TokenMetaMapper;
import com.fourmeme.bot.service.TokenMetaService;
import com.fourmeme.bot.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenMetaServiceImpl implements TokenMetaService {

    private final TokenMetaMapper tokenMetaMapper;
    private final TradeService tradeService;

    /** 数据来源常量 */
    private static final String SOURCE_CHAIN = "CHAIN";
    private static final String SOURCE_MANUAL = "MANUAL";

    @Override
    public TokenMeta getOrFetch(String tokenAddress) {
        if (tokenAddress == null || tokenAddress.trim().isEmpty()) {
            return null;
        }
        String addr = tokenAddress.trim().toLowerCase();

        // 1. 优先从数据库读取
        TokenMeta cached = getByAddress(addr);
        if (cached != null && cached.getTokenName() != null) {
            return cached;
        }

        // 2. 数据库没有或字段不全，从链上拉取
        TokenMeta fetched = fetchFromChain(addr);
        if (fetched == null) {
            return cached; // 可能返回 null
        }

        // 3. 写库（存在则更新，不存在则插入）
        if (cached == null) {
            tokenMetaMapper.insert(fetched);
            log.info("代币元数据入库: address={} symbol={} market={}", fetched.getTokenAddress(), fetched.getTokenName(), fetched.getMarketPair());
        } else {
            fetched.setId(cached.getId());
            tokenMetaMapper.updateById(fetched);
            log.info("代币元数据更新: address={} symbol={}", addr, fetched.getTokenName());
        }
        return fetched;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenMeta saveManual(TokenMeta metadata) {
        if (metadata == null || metadata.getTokenAddress() == null) {
            throw new IllegalArgumentException("代币地址不能为空");
        }
        String addr = metadata.getTokenAddress().trim().toLowerCase();
        metadata.setTokenAddress(addr);
        metadata.setSource(SOURCE_MANUAL);

        TokenMeta existing = getByAddress(addr);
        if (existing == null) {
            tokenMetaMapper.insert(metadata);
        } else {
            metadata.setId(existing.getId());
            tokenMetaMapper.updateById(metadata);
        }
        log.info("人工保存代币元数据: address={} symbol={}", addr, metadata.getTokenName());
        return metadata;
    }

    @Override
    public TokenMeta getByAddress(String tokenAddress) {
        if (tokenAddress == null || tokenAddress.trim().isEmpty()) {
            return null;
        }
        String addr = tokenAddress.trim().toLowerCase();
        return tokenMetaMapper.selectOne(
                new LambdaQueryWrapper<TokenMeta>()
                        .eq(TokenMeta::getTokenAddress, addr));
    }

    // ==================== 私有方法 ====================

    /**
     * 从链上拉取代币元数据
     */
    private TokenMeta fetchFromChain(String tokenAddress) {
        try {
            // 优先获取简称，拿不到再用全名兜底
            String displayName = tradeService.getTokenSymbol(tokenAddress);
            if (displayName == null || displayName.trim().isEmpty()) {
                log.debug("symbol 为空，尝试用 name 兜底: {}", tokenAddress);
                displayName = tradeService.getTokenName(tokenAddress);
            }

            if (displayName == null || displayName.trim().isEmpty()) {
                log.warn("链上获取代币简称失败: {}", tokenAddress);
                return null;
            }

            Integer decimals = tradeService.getTokenDecimals(tokenAddress);
            MarketPair market = null;
            try {
                market = tradeService.detectMarket(tokenAddress);
            } catch (Exception e) {
                log.warn("识别代币市场失败: {} - {}", tokenAddress, e.getMessage());
            }

            TokenMeta metadata = new TokenMeta();
            metadata.setTokenAddress(tokenAddress);
            metadata.setTokenName(displayName.trim());
            metadata.setDecimals(decimals);
            metadata.setMarketPair(market != null ? market.getName() : null);
            metadata.setSource(SOURCE_CHAIN);

            log.info("从链上获取代币元数据成功: address={} symbol={} decimals={} market={}", tokenAddress, metadata.getTokenName(), decimals, metadata.getMarketPair());
            return metadata;

        } catch (Exception e) {
            log.error("拉取代币元数据异常: {} - {}", tokenAddress, e.getMessage(), e);
            return null;
        }
    }

}