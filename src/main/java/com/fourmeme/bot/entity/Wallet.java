package com.fourmeme.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wallet")
public class Wallet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String address;
    private String privateKeyEncrypted;
    private String mnemonicEncrypted;
    private BigDecimal bnbBalance;
    private BigDecimal usdtBalance;
    private Integer status;
    private Long lastTradeAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}