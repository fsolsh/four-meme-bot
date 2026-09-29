package com.fourmeme.bot.response;

import com.fourmeme.bot.entity.Wallet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "钱包视图（脱敏，不含私钥和助记词）")
public class WalletResponse {

    @Schema(description = "钱包ID", example = "1")
    private Long id;

    @Schema(description = "钱包地址", example = "0xAbC123...")
    private String address;

    @Schema(description = "BNB余额", example = "0.3")
    private BigDecimal bnbBalance;

    @Schema(description = "USDT余额", example = "120.5")
    private BigDecimal usdtBalance;

    @Schema(description = "状态：1=启用 0=禁用", example = "1")
    private Integer status;

    @Schema(description = "上次交易时间戳（毫秒）")
    private Long lastTradeAt;

    @Schema(description = "是否已存有助记词（可备份）")
    private Boolean hasMnemonic;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "持仓代币种类数", example = "3")
    private Long tokenCount;

    /**
     * 不带 tokenCount 的转换（tokenCount 默认为 0）
     */
    public static WalletResponse from(Wallet wallet) {
        return from(wallet, 0L);
    }

    /**
     * 带 tokenCount 的转换
     */
    public static WalletResponse from(Wallet wallet, Long tokenCount) {
        if (wallet == null) return null;
        WalletResponse dto = new WalletResponse();
        dto.setId(wallet.getId());
        dto.setAddress(wallet.getAddress());
        dto.setBnbBalance(wallet.getBnbBalance());
        dto.setUsdtBalance(wallet.getUsdtBalance());
        dto.setStatus(wallet.getStatus());
        dto.setLastTradeAt(wallet.getLastTradeAt());
        dto.setHasMnemonic(wallet.getMnemonicEncrypted() != null
                && !wallet.getMnemonicEncrypted().isEmpty());
        dto.setCreatedAt(wallet.getCreatedAt());
        dto.setTokenCount(tokenCount != null ? tokenCount : 0L);
        return dto;
    }
}