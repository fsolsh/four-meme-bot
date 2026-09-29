package com.fourmeme.bot.service;

import com.fourmeme.bot.entity.Wallet;
import org.web3j.crypto.Credentials;

/**
 * 钱包凭证转换服务
 * 独立出来，打断 TradeService 与 WalletService 之间的循环依赖
 */
public interface CredentialsService {

    /** 从钱包实体获取链上凭证 */
    Credentials getCredentials(Wallet wallet);

    /** 从数据库中的助记词重新派生凭证 */
    Credentials getCredentialsFromMnemonic(Long walletId);
}