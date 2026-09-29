package com.fourmeme.bot.dto;

import com.fourmeme.bot.entity.Wallet;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletDTO {

    private Long id;
    private String address;
    private BigDecimal bnbBalance;
    private BigDecimal usdtBalance;
    private Integer status;
    private Long lastTradeAt;
    private LocalDateTime createdAt;

    /** 是否已存助记词（有=可导出，无=不能导出） */
    private Boolean hasMnemonic;

    /** 持仓代币种类数 */
    private Long tokenCount;

    /**
     * 从实体转换，tokenCount 默认 0
     */
    public static WalletDTO from(Wallet wallet) {
        return from(wallet, 0L);
    }

    /**
     * 从实体转换，带持仓种类数
     */
    public static WalletDTO from(Wallet wallet, Long tokenCount) {
        if (wallet == null) return null;
        WalletDTO dto = new WalletDTO();
        dto.setId(wallet.getId());
        dto.setAddress(wallet.getAddress());
        dto.setBnbBalance(wallet.getBnbBalance());
        dto.setUsdtBalance(wallet.getUsdtBalance());
        dto.setStatus(wallet.getStatus());
        dto.setLastTradeAt(wallet.getLastTradeAt());
        dto.setCreatedAt(wallet.getCreatedAt());
        dto.setHasMnemonic(wallet.getMnemonicEncrypted() != null
                && !wallet.getMnemonicEncrypted().isEmpty());
        dto.setTokenCount(tokenCount != null ? tokenCount : 0L);
        return dto;
    }
}