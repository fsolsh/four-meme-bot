package com.fourmeme.bot.service;

import com.fourmeme.bot.entity.WalletTokenBalance;

import java.math.BigDecimal;
import java.util.List;

public interface WalletTokenBalanceService {

    /** 插入或更新 */
    void upsert(Long walletId, String tokenAddress, String tokenName, BigDecimal balance);

    /** 查询某钱包的所有代币持仓 */
    List<WalletTokenBalance> listByWallet(Long walletId);

    /** 查询某钱包的持仓种类数 */
    long countByWallet(Long walletId);

    /** 删除某钱包的所有持仓记录 */
    void deleteByWallet(Long walletId);
}