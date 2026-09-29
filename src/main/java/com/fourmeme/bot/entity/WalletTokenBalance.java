package com.fourmeme.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wallet_token_balance")
public class WalletTokenBalance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long walletId;
    private String tokenAddress;
    private String tokenName;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}