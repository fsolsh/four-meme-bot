package com.fourmeme.bot.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/**
 * Four.meme 官方 API 客户端
 * 仅调用官方免费接口，无需付费 API Key
 */
public interface FourMemeApiService {

    /**
     * 获取平台公共配置（包含毕业阈值等）
     * 免费接口，无需认证
     *
     * @return 配置 JSON，失败返回 null
     */
    JsonNode getPublicConfig();

    /**
     * 通过代币地址获取元数据
     * 免费接口，可选认证
     *
     * @param tokenAddress 代币合约地址
     * @param accessToken  可选访问令牌
     * @return 代币元数据 JSON，失败返回 null
     */
    JsonNode getTokenByAddress(String tokenAddress, String accessToken);

    /**
     * 通过官方 API 检查代币是否已毕业
     *
     * @param tokenAddress 代币合约地址
     * @return 已毕业返回 true，否则返回 false
     */
    boolean isGraduatedViaApi(String tokenAddress);

    /**
     * 获取毕业阈值（BNB 交易对）
     * 优先从官方 API 获取，失败则返回默认值 18
     *
     * @return 毕业阈值（BNB）
     */
    BigDecimal getGraduationThresholdBnb();
}