package com.fourmeme.bot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Web3jConfig {

    private final Web3Properties props;

    /**
     * 节点轮询计数器
     */
    private final AtomicInteger rpcIndex = new AtomicInteger(0);

    /**
     * 默认 Web3j Bean，使用节点池中的第一个节点
     */
    @Bean
    public Web3j web3j() {
        String rpcUrl = nextRpcUrl();
        log.info("初始化 Web3j，RPC 节点: {}", rpcUrl);
        return buildWeb3j(rpcUrl);
    }

    /**
     * 获取下一个 RPC 节点地址（轮询）
     */
    public String nextRpcUrl() {
        List<String> pool = props.getRpcPool();
        if (pool == null || pool.isEmpty()) {
            throw new IllegalStateException("web3.rpc-pool 未配置");
        }
        int idx = Math.abs(rpcIndex.getAndIncrement() % pool.size());
        return pool.get(idx);
    }

    /**
     * 根据指定 URL 构建 Web3j 实例
     * 用于故障切换时创建新连接
     */
    public Web3j buildWeb3j(String rpcUrl) {
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(props.getConnectTimeoutSeconds(), TimeUnit.SECONDS).readTimeout(props.getReadTimeoutSeconds(), TimeUnit.SECONDS).writeTimeout(props.getWriteTimeoutSeconds(), TimeUnit.SECONDS).retryOnConnectionFailure(true).build();
        HttpService httpService = new HttpService(rpcUrl, client, false);
        return Web3j.build(httpService);
    }

    /**
     * 链 ID Bean
     */
    @Bean
    public long chainId() {
        Integer id = props.getChainId();
        if (id == null) {
            throw new IllegalStateException("web3.chain-id 未配置");
        }
        return id.longValue();
    }
}