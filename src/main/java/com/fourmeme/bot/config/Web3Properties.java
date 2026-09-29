package com.fourmeme.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "web3")
public class Web3Properties {

    /** RPC 节点池 */
    private List<String> rpcPool;

    /** 链 ID（BSC 主网 56，测试网 97） */
    private Integer chainId;

    /** 连接超时（秒） */
    private Integer connectTimeoutSeconds = 30;

    /** 读取超时（秒） */
    private Integer readTimeoutSeconds = 60;

    /** 写入超时（秒） */
    private Integer writeTimeoutSeconds = 60;

    /** RPC 调用失败重试次数 */
    private Integer maxRetries = 3;

    /** 重试间隔（毫秒） */
    private Long retryDelayMs = 1000L;
}