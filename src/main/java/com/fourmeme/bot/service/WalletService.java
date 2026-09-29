package com.fourmeme.bot.service;

import com.fourmeme.bot.entity.Wallet;
import org.web3j.crypto.Credentials;

import java.util.List;

/**
 * 钱包管理服务
 * 负责钱包的创建、导入、派生、查询、余额刷新、启停控制，以及助记词/私钥的恢复与导出
 */
public interface WalletService {

    // ==================== 创建钱包 ====================

    /**
     * 创建新钱包（仅私钥，无助记词）
     * ⚠️ 建议生产环境使用 {@link #createWalletWithMnemonic()}，便于备份和恢复
     *
     * @return 新创建的钱包实体
     */
    Wallet createWallet() throws Exception;

    /**
     * 创建带助记词的新钱包（推荐）
     * 生成 BIP39 助记词，私钥和助记词均加密存储
     *
     * @return 新创建的钱包实体
     */
    Wallet createWalletWithMnemonic() throws Exception;

    // ==================== 查询钱包 ====================

    /**
     * 获取所有启用的钱包
     *
     * @return 状态为启用的钱包列表
     */
    List<Wallet> getAllWallets(int status);

    /**
     * 根据ID查询钱包
     *
     * @param id 钱包ID
     * @return 钱包实体，不存在时返回 null
     */
    Wallet getWalletById(Long id);

    /**
     * 根据地址查询钱包
     *
     * @param address 钱包地址（带 0x 前缀）
     * @return 钱包实体，不存在时返回 null
     */
    Wallet getWalletByAddress(String address);

    // ==================== 凭证获取 ====================

    /**
     * 从加密私钥获取 Credentials（日常交易使用，性能最优）
     *
     * @param wallet 钱包实体
     * @return Web3j Credentials
     */
    Credentials getCredentials(Wallet wallet);

    /**
     * 从数据库中存储的加密助记词恢复 Credentials
     * 用于私钥损坏、钱包迁移、重新派生地址等场景
     *
     * @param walletId 钱包ID
     * @return Web3j Credentials
     * @throws RuntimeException 钱包不存在或未存储助记词时抛出
     */
    Credentials getCredentialsFromMnemonic(Long walletId);

    // ==================== 助记词导出与恢复 ====================

    /**
     * 导出助记词（明文）
     * ⚠️ 仅应在用户主动请求备份时调用，切勿写入日志
     *
     * @param walletId 钱包ID
     * @return 明文助记词
     * @throws RuntimeException 钱包不存在或未存储助记词时抛出
     */
    String exportMnemonic(Long walletId);

    /**
     * 通过助记词恢复私钥（不操作数据库）
     *
     * @param mnemonic 助记词
     * @return 带 0x 前缀的私钥字符串
     * @throws RuntimeException 助记词无效时抛出
     */
    String recoverPrivateKeyFromMnemonic(String mnemonic);

    /**
     * 通过助记词恢复地址（不操作数据库）
     *
     * @param mnemonic 助记词
     * @return 带 0x 前缀的钱包地址
     * @throws RuntimeException 助记词无效时抛出
     */
    String recoverAddressFromMnemonic(String mnemonic);

    // ==================== 导入钱包 ====================

    /**
     * 通过助记词导入钱包到数据库
     * 用于用户已有助记词，想导入本系统统一管理
     *
     * @param mnemonic 助记词
     * @return 导入后的钱包实体
     * @throws RuntimeException 助记词无效或钱包已存在时抛出
     */
    Wallet importWalletFromMnemonic(String mnemonic) throws Exception;

    // ==================== 多地址派生 ====================

    /**
     * 通过助记词 + 派生索引生成钱包
     * 路径: m/44'/60'/0'/0/{index}
     *
     * @param mnemonic 助记词
     * @param index    派生索引，从 0 开始
     * @return 派生出的钱包实体（已存在时返回已有记录）
     * @throws RuntimeException 助记词无效或索引为负数时抛出
     */
    Wallet deriveWalletFromMnemonic(String mnemonic, int index) throws Exception;

    /**
     * 从同一个助记词批量派生 N 个钱包
     * 用于多钱包 Volume Bot 场景，只需备份一个助记词即可恢复全部地址
     *
     * @param mnemonic 助记词
     * @param count    派生数量（1-100）
     * @return 派生出的钱包列表
     * @throws RuntimeException 助记词无效或数量超出范围时抛出
     */
    List<Wallet> deriveWalletsFromMnemonic(String mnemonic, int count) throws Exception;

    // ==================== 余额管理 ====================

    /**
     * 刷新单个钱包的 BNB 余额
     *
     * @param wallet 钱包实体
     */
    void refreshBalance(Wallet wallet) throws Exception;

    /**
     * 批量刷新所有启用钱包的余额
     * 单个钱包失败不会中断整体流程
     */
    void refreshAllBalances();

    // ==================== 启停控制 ====================

    /**
     * 启用钱包
     *
     * @param id 钱包ID
     * @throws RuntimeException 钱包不存在时抛出
     */
    void enableWallet(Long id);

    /**
     * 禁用钱包
     *
     * @param id 钱包ID
     * @throws RuntimeException 钱包不存在时抛出
     */
    void disableWallet(Long id);
}