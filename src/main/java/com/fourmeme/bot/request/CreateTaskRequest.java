package com.fourmeme.bot.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "创建Volume Bot任务")
public class CreateTaskRequest {
    @Schema(description = "目标代币合约地址", required = true, example = "0x6861af7d3f399869bfe9592b558a9ac3efa84444")
    private String tokenAddress;

    @Schema(description = "单笔最小买入金额", required = true, example = "0.001")
    private BigDecimal minBuyAmount;

    @Schema(description = "单笔最大买入金额", required = true, example = "0.005")
    private BigDecimal maxBuyAmount;

    @Schema(description = "最小交易间隔（秒）", example = "15")
    private Integer minIntervalSec = 10;

    @Schema(description = "最大交易间隔（秒）", example = "30")
    private Integer maxIntervalSec = 30;

    @Schema(description = "买入权重（0-100，表示买入的概率）", example = "60")
    private Integer buyWeight = 60;

    @Schema(description = "每轮最多交易次数", example = "1")
    private Integer maxTradesPerRound = 1;

    @Schema(description = "毕业阈值", example = "18")
    private BigDecimal graduationThreshold = BigDecimal.valueOf(18);
}