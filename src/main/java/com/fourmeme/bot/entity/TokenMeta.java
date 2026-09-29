package com.fourmeme.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("token_meta")
public class TokenMeta {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 代币合约地址（小写存储） */
    private String tokenAddress;

    /** 代币简称 */
    private String tokenName;

    /** 精度 */
    private Integer decimals;

    /** 市场交易对：BNB / USDT */
    private String marketPair;

    /** 数据来源：CHAIN（链上自动获取）/ MANUAL（人工维护） */
    private String source;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}