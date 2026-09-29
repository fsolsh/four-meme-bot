package com.fourmeme.bot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fourmeme.bot.entity.BotTask;
import com.fourmeme.bot.entity.TokenMeta;
import com.fourmeme.bot.entity.Wallet;
import com.fourmeme.bot.enums.MarketPair;
import com.fourmeme.bot.mapper.WalletMapper;
import com.fourmeme.bot.service.*;
import com.fourmeme.bot.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final AesUtil aesUtil;
    private final Web3j web3j;
    private final BotTaskService botTaskService;
    private final TokenMetaService tokenMetaService;
    private final WalletTokenBalanceService walletTokenBalanceService;
    private final TradeService tradeService;
    private final CredentialsService credentialsService;   // ✅ 新增

    private static final int MIN_DERIVE_COUNT = 1;
    private static final int MAX_DERIVE_COUNT = 100;

    /**
     * 手写构造器：现在没有循环依赖，无需 @Lazy
     *
     * 依赖链（单向，无环）：
     *   WalletService → TradeService → CredentialsService
     *   WalletService → TokenMetaService → TradeService
     */
    public WalletServiceImpl(WalletMapper walletMapper,
                             AesUtil aesUtil,
                             Web3j web3j,
                             BotTaskService botTaskService,
                             TokenMetaService tokenMetaService,
                             WalletTokenBalanceService walletTokenBalanceService,
                             TradeService tradeService,
                             CredentialsService credentialsService) {
        this.walletMapper = walletMapper;
        this.aesUtil = aesUtil;
        this.web3j = web3j;
        this.botTaskService = botTaskService;
        this.tokenMetaService = tokenMetaService;
        this.walletTokenBalanceService = walletTokenBalanceService;
        this.tradeService = tradeService;
        this.credentialsService = credentialsService;
    }

    // ==================== 创建钱包 ====================

    @Override
    public Wallet createWallet() throws Exception {
        ECKeyPair keyPair = Keys.createEcKeyPair();
        String privateKey = Numeric.toHexStringWithPrefix(keyPair.getPrivateKey());
        String address = "0x" + Keys.getAddress(keyPair.getPublicKey());

        Wallet wallet = new Wallet();
        wallet.setAddress(address);
        wallet.setPrivateKeyEncrypted(aesUtil.encrypt(privateKey));
        wallet.setStatus(1);
        wallet.setLastTradeAt(0L);
        wallet.setBnbBalance(BigDecimal.ZERO);
        wallet.setUsdtBalance(BigDecimal.ZERO);
        walletMapper.insert(wallet);

        log.info("创建钱包成功(无助记词备份): address={}", address);
        return wallet;
    }

    @Override
    public Wallet createWalletWithMnemonic() throws Exception {
        byte[] initialEntropy = new byte[16];
        new SecureRandom().nextBytes(initialEntropy);

        String mnemonic = MnemonicUtils.generateMnemonic(initialEntropy);

        Credentials credentials = WalletUtils.loadBip39Credentials(null, mnemonic);

        String address = credentials.getAddress();
        ECKeyPair keyPair = credentials.getEcKeyPair();
        String privateKey = Numeric.toHexStringWithPrefix(keyPair.getPrivateKey());

        Wallet wallet = new Wallet();
        wallet.setAddress(address);
        wallet.setPrivateKeyEncrypted(aesUtil.encrypt(privateKey));
        wallet.setMnemonicEncrypted(aesUtil.encrypt(mnemonic));
        wallet.setStatus(1);
        wallet.setLastTradeAt(0L);
        wallet.setBnbBalance(BigDecimal.ZERO);
        wallet.setUsdtBalance(BigDecimal.ZERO);
        walletMapper.insert(wallet);

        log.info("创建钱包成功(带助记词): address={}", address);
        return wallet;
    }

    // ==================== 查询钱包 ====================

    @Override
    public List<Wallet> getAllWallets(int status) {
        return walletMapper.selectList(
                new LambdaQueryWrapper<Wallet>().eq(status != 0, Wallet::getStatus, status));
    }

    @Override
    public Wallet getWalletById(Long id) {
        if (id == null) {
            return null;
        }
        return walletMapper.selectById(id);
    }

    @Override
    public Wallet getWalletByAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        return walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getAddress, address));
    }

    // ==================== 凭证获取（委托给 CredentialsService） ====================

    @Override
    public Credentials getCredentials(Wallet wallet) {
        // ✅ 委托给独立的 CredentialsService，避免在此处重复加解密逻辑
        return credentialsService.getCredentials(wallet);
    }

    @Override
    public Credentials getCredentialsFromMnemonic(Long walletId) {
        return credentialsService.getCredentialsFromMnemonic(walletId);
    }

    // ==================== 助记词导出与恢复 ====================

    @Override
    public String exportMnemonic(Long walletId) {
        Wallet wallet = walletMapper.selectById(walletId);
        if (wallet == null) {
            throw new IllegalArgumentException("钱包不存在: " + walletId);
        }
        if (wallet.getMnemonicEncrypted() == null
                || wallet.getMnemonicEncrypted().isEmpty()) {
            throw new IllegalStateException("该钱包未存储助记词: " + walletId);
        }
        return aesUtil.decrypt(wallet.getMnemonicEncrypted());
    }

    @Override
    public String recoverPrivateKeyFromMnemonic(String mnemonic) {
        validateMnemonic(mnemonic);
        Credentials credentials = WalletUtils.loadBip39Credentials(null, mnemonic);
        return Numeric.toHexStringWithPrefix(credentials.getEcKeyPair().getPrivateKey());
    }

    @Override
    public String recoverAddressFromMnemonic(String mnemonic) {
        validateMnemonic(mnemonic);
        return WalletUtils.loadBip39Credentials(null, mnemonic).getAddress();
    }

    // ==================== 导入钱包 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Wallet importWalletFromMnemonic(String mnemonic) throws Exception {
        validateMnemonic(mnemonic);

        Credentials credentials = WalletUtils.loadBip39Credentials(null, mnemonic);
        String address = credentials.getAddress();
        String privateKey = Numeric.toHexStringWithPrefix(
                credentials.getEcKeyPair().getPrivateKey());

        Wallet existing = getWalletByAddress(address);
        if (existing != null) {
            throw new IllegalStateException("该助记词对应的钱包已存在: " + address);
        }

        Wallet wallet = new Wallet();
        wallet.setAddress(address);
        wallet.setPrivateKeyEncrypted(aesUtil.encrypt(privateKey));
        wallet.setMnemonicEncrypted(aesUtil.encrypt(mnemonic));
        wallet.setStatus(1);
        wallet.setLastTradeAt(0L);
        wallet.setBnbBalance(BigDecimal.ZERO);
        wallet.setUsdtBalance(BigDecimal.ZERO);
        walletMapper.insert(wallet);

        log.info("导入钱包成功: address={}", address);
        return wallet;
    }

    // ==================== 多地址派生 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Wallet deriveWalletFromMnemonic(String mnemonic, int index) throws Exception {
        validateMnemonic(mnemonic);
        if (index < 0) {
            throw new IllegalArgumentException("派生索引必须 >= 0");
        }

        byte[] seed = MnemonicUtils.generateSeed(mnemonic, null);
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(seed);

        final int[] path = {
                44 | Bip32ECKeyPair.HARDENED_BIT,
                60 | Bip32ECKeyPair.HARDENED_BIT,
                0  | Bip32ECKeyPair.HARDENED_BIT,
                0,
                index
        };

        Bip32ECKeyPair derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, path);
        Credentials credentials = Credentials.create(derivedKeyPair);
        String address = credentials.getAddress();
        String privateKey = Numeric.toHexStringWithPrefix(
                credentials.getEcKeyPair().getPrivateKey());

        Wallet existing = getWalletByAddress(address);
        if (existing != null) {
            log.info("派生钱包已存在，直接返回: address={} index={}", address, index);
            return existing;
        }

        Wallet wallet = new Wallet();
        wallet.setAddress(address);
        wallet.setPrivateKeyEncrypted(aesUtil.encrypt(privateKey));
        wallet.setMnemonicEncrypted(aesUtil.encrypt(mnemonic));
        wallet.setStatus(1);
        wallet.setLastTradeAt(0L);
        wallet.setBnbBalance(BigDecimal.ZERO);
        wallet.setUsdtBalance(BigDecimal.ZERO);
        walletMapper.insert(wallet);

        log.info("从助记词派生钱包成功: address={} index={}", address, index);
        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Wallet> deriveWalletsFromMnemonic(String mnemonic, int count) throws Exception {
        validateMnemonic(mnemonic);
        if (count < MIN_DERIVE_COUNT || count > MAX_DERIVE_COUNT) {
            throw new IllegalArgumentException(
                    "派生数量必须在 " + MIN_DERIVE_COUNT + "-" + MAX_DERIVE_COUNT + " 之间");
        }

        List<Wallet> wallets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            wallets.add(deriveWalletFromMnemonic(mnemonic, i));
        }
        log.info("从助记词批量派生钱包成功: count={}", wallets.size());
        return wallets;
    }

    // ==================== 余额管理 ====================

    @Override
    public void refreshBalance(Wallet wallet) throws Exception {
        if (wallet == null || wallet.getAddress() == null) {
            throw new IllegalArgumentException("钱包或地址不能为空");
        }

        try {
            wallet.setBnbBalance(MarketPair.BNB.getBalance(web3j, wallet.getAddress()));
        } catch (Exception e) {
            log.warn("查询BNB余额失败, address={}, error={}", wallet.getAddress(), e.getMessage());
        }

        try {
            wallet.setUsdtBalance(MarketPair.USDT.getBalance(web3j, wallet.getAddress()));
        } catch (Exception e) {
            log.warn("查询USDT余额失败, address={}, error={}", wallet.getAddress(), e.getMessage());
        }

        refreshTokenBalances(wallet);

        walletMapper.updateById(wallet);
    }

    private void refreshTokenBalances(Wallet wallet) {
        List<BotTask> tasks = botTaskService.listAll();
        if (tasks == null || tasks.isEmpty()) return;

        Set<String> tokenAddresses = new HashSet<>();
        for (BotTask task : tasks) {
            if (task.getTokenAddress() != null) {
                tokenAddresses.add(task.getTokenAddress().toLowerCase());
            }
        }

        for (String tokenAddress : tokenAddresses) {
            try {
                TokenMeta meta = tokenMetaService.getOrFetch(tokenAddress);
                Integer decimals = (meta != null) ? meta.getDecimals() : 18;
                String tokenName = (meta != null) ? meta.getTokenName() : null;

                BigDecimal balance = tradeService.getErc20Balance(
                        tokenAddress, wallet.getAddress(), decimals);

                walletTokenBalanceService.upsert(
                        wallet.getId(), tokenAddress, tokenName, balance);
            } catch (Exception e) {
                log.warn("刷新代币持仓失败 wallet={} token={}: {}",
                        wallet.getAddress(), tokenAddress, e.getMessage());
            }
        }
    }

    @Override
    public void refreshAllBalances() {
        List<Wallet> wallets = getAllWallets(0);
        int success = 0;
        int failed = 0;
        for (Wallet wallet : wallets) {
            try {
                refreshBalance(wallet);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("刷新余额失败 address={}: {}", wallet.getAddress(), e.getMessage());
            }
        }
        log.info("批量刷新余额完成: 成功={} 失败={}", success, failed);
    }

    // ==================== 启停控制 ====================

    @Override
    public void enableWallet(Long id) {
        Wallet wallet = walletMapper.selectById(id);
        if (wallet == null) {
            throw new IllegalArgumentException("钱包不存在: " + id);
        }
        wallet.setStatus(1);
        walletMapper.updateById(wallet);
        log.info("启用钱包: id={} address={}", id, wallet.getAddress());
    }

    @Override
    public void disableWallet(Long id) {
        Wallet wallet = walletMapper.selectById(id);
        if (wallet == null) {
            throw new IllegalArgumentException("钱包不存在: " + id);
        }
        wallet.setStatus(0);
        walletMapper.updateById(wallet);
        log.info("禁用钱包: id={} address={}", id, wallet.getAddress());
    }

    // ==================== 私有工具方法 ====================

    private void validateMnemonic(String mnemonic) {
        if (mnemonic == null || mnemonic.trim().isEmpty()) {
            throw new IllegalArgumentException("助记词不能为空");
        }
        if (!MnemonicUtils.validateMnemonic(mnemonic.trim())) {
            throw new IllegalArgumentException("助记词无效，请检查单词拼写和数量");
        }
    }
}