package com.fourmeme.bot.service.impl;

import com.fourmeme.bot.entity.Wallet;
import com.fourmeme.bot.mapper.WalletMapper;
import com.fourmeme.bot.service.CredentialsService;
import com.fourmeme.bot.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialsServiceImpl implements CredentialsService {

    private final WalletMapper walletMapper;
    private final AesUtil aesUtil;

    @Override
    public Credentials getCredentials(Wallet wallet) {
        if (wallet == null) {
            throw new IllegalArgumentException("钱包不能为空");
        }
        if (wallet.getPrivateKeyEncrypted() == null
                || wallet.getPrivateKeyEncrypted().isEmpty()) {
            throw new IllegalStateException("钱包未存储私钥: " + wallet.getId());
        }
        String privateKey = aesUtil.decrypt(wallet.getPrivateKeyEncrypted());
        return Credentials.create(privateKey);
    }

    @Override
    public Credentials getCredentialsFromMnemonic(Long walletId) {
        Wallet wallet = walletMapper.selectById(walletId);
        if (wallet == null) {
            throw new IllegalArgumentException("钱包不存在: " + walletId);
        }
        if (wallet.getMnemonicEncrypted() == null
                || wallet.getMnemonicEncrypted().isEmpty()) {
            throw new IllegalStateException("该钱包未存储助记词，无法恢复: " + walletId);
        }
        String mnemonic = aesUtil.decrypt(wallet.getMnemonicEncrypted());
        return WalletUtils.loadBip39Credentials(null, mnemonic);
    }
}