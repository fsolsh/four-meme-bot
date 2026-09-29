package com.fourmeme.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bot_task")
public class BotTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String marketPair; // 市场标识，如 "BNB" 或 "USDT"
    private String tokenAddress;
    private String tokenName;
    private BigDecimal minBuyAmount;
    private BigDecimal maxBuyAmount;
    private Integer minIntervalSec;
    private Integer maxIntervalSec;
    private Integer buyWeight;
    private Integer maxTradesPerRound;
    private Integer isRunning;
    private Integer graduated;
    private BigDecimal graduationThreshold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}