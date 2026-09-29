package com.fourmeme.bot.service;

import com.fourmeme.bot.entity.BotTask;
import com.fourmeme.bot.entity.TokenMeta;
import com.fourmeme.bot.entity.Wallet;
import com.fourmeme.bot.enums.MarketPair;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 链上交易服务
 * 负责 Four.meme 联合曲线交易、PancakeSwap 交易、代币授权、毕业检测
 */
public interface TradeService {

    // ==================== 毕业前：Four.meme 联合曲线 ====================

    /**
     * 在 Four.meme 联合曲线上买入代币
     * 函数签名: buy(address,address,uint256,address,uint256)
     *
     * @param wallet    交易钱包
     * @param task      任务配置
     * @param bnbAmount 买入金额（BNB）
     * @param gasPrice  Gas Price
     * @return 交易哈希
     */
    String buyOnBondingCurve(Wallet wallet, BotTask task,
                             BigDecimal bnbAmount, BigInteger gasPrice) throws Exception;

    /**
     * 在 Four.meme 联合曲线上卖出代币
     * 函数签名: sell(address,address,uint256,uint256)
     *
     * @param wallet      交易钱包
     * @param task        任务配置
     * @param tokenAmount 卖出代币数量
     * @param gasPrice    Gas Price
     * @return 交易哈希
     */
    String sellOnBondingCurve(Wallet wallet, BotTask task,
                              BigDecimal tokenAmount, BigInteger gasPrice) throws Exception;

    // ==================== 毕业后：PancakeSwap V2 ====================

    /**
     * 在 PancakeSwap V2 上买入代币
     * 函数: swapExactETHForTokens
     */
    String buyOnPancake(Wallet wallet, BotTask task,
                        BigDecimal bnbAmount, BigInteger gasPrice) throws Exception;

    /**
     * 在 PancakeSwap V2 上卖出代币
     * 函数: swapExactTokensForETH
     */
    String sellOnPancake(Wallet wallet, BotTask task,
                         BigDecimal tokenAmount, BigInteger gasPrice) throws Exception;

    // ==================== 工具方法 ====================

    /**
     * 根据毕业状态自动路由交易
     * graduated=0 → Four.meme 联合曲线
     * graduated=1 → PancakeSwap
     */
    String executeTrade(Wallet wallet, BotTask task,
                        boolean isBuy, BigDecimal amount,
                        BigInteger gasPrice) throws Exception;

    /**
     * 授权代币给指定合约（ERC20 approve）
     */
    String approveToken(Credentials credentials, String tokenAddress,
                        String spender, BigInteger gasPrice) throws Exception;


    /**
     * 查询钱包持有的某个 ERC20 代币余额
     *
     * @param tokenAddress  代币合约地址
     * @param walletAddress 钱包地址
     * @param decimals      代币精度，null 或 <=0 时按 18 处理
     */
    BigDecimal getErc20Balance(String tokenAddress, String walletAddress, Integer decimals);

    /**
     * 查询 PancakeSwap Factory 中是否已存在该代币的交易对
     *
     * @param tokenAddress      代币合约地址
     * @param quoteTokenAddress 交易对代币地址（BNB 市场传 WBNB，USDT 市场传 USDT）
     */
    boolean isTokenOnPancakeSwap(String tokenAddress, String quoteTokenAddress);

    /**
     * 便捷方法：根据任务的 marketPair 自动选择 quoteToken
     */
    boolean isTokenOnPancakeSwap(BotTask task);

    /**
     * 获取随机 Gas Price（基于当前市场价 ±20% 浮动）
     */
    BigInteger getRandomGasPrice();

    /**
     * 从链上获取代币简称
     */
    String getTokenSymbol(String tokenAddress);

    /**
     * 从链上获取代币名称
     */
    String getTokenName(String tokenAddress);

    /**
     * 从链上获取代币精度
     */
    Integer getTokenDecimals(String tokenAddress);

    /**
     * 自动识别代币所属市场
     */
    MarketPair detectMarket(String tokenAddress);
}