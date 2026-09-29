package com.fourmeme.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("trade_record")
public class TradeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long walletId;
    private String tokenAddress;
    private String tokenName;
    private String quoteName;
    private String tradeType;
    private BigDecimal quoteAmount;
    private BigDecimal tokenAmount;
    private String txHash;
    private Integer status;
    private String errorMsg;
    private Long gasPrice;
    private String stage;
    private LocalDateTime createdAt;
}