package com.fourmeme.bot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fourmeme.bot.entity.WalletTokenBalance;
import com.fourmeme.bot.mapper.WalletTokenBalanceMapper;
import com.fourmeme.bot.service.WalletTokenBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletTokenBalanceServiceImpl implements WalletTokenBalanceService {

    private final WalletTokenBalanceMapper mapper;

    @Override
    public void upsert(Long walletId, String tokenAddress, String tokenName, BigDecimal balance) {
        String addr = tokenAddress.toLowerCase();
        WalletTokenBalance existing = mapper.selectOne(
                new LambdaQueryWrapper<WalletTokenBalance>()
                        .eq(WalletTokenBalance::getWalletId, walletId)
                        .eq(WalletTokenBalance::getTokenAddress, addr));
        if (existing == null) {
            WalletTokenBalance e = new WalletTokenBalance();
            e.setWalletId(walletId);
            e.setTokenAddress(addr);
            e.setTokenName(tokenName);
            e.setBalance(balance);
            mapper.insert(e);
        } else {
            existing.setBalance(balance);
            existing.setTokenName(tokenName);
            mapper.updateById(existing);
        }
    }

    @Override
    public List<WalletTokenBalance> listByWallet(Long walletId) {
        return mapper.selectList(
                new LambdaQueryWrapper<WalletTokenBalance>()
                        .eq(WalletTokenBalance::getWalletId, walletId)
                        .orderByDesc(WalletTokenBalance::getBalance));
    }

    @Override
    public long countByWallet(Long walletId) {
        return mapper.selectCount(
                new LambdaQueryWrapper<WalletTokenBalance>()
                        .eq(WalletTokenBalance::getWalletId, walletId));
    }

    @Override
    public void deleteByWallet(Long walletId) {
        mapper.delete(new LambdaQueryWrapper<WalletTokenBalance>()
                        .eq(WalletTokenBalance::getWalletId, walletId));
    }
}