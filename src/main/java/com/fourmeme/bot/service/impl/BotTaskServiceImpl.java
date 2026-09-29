package com.fourmeme.bot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fourmeme.bot.entity.BotTask;
import com.fourmeme.bot.entity.TokenMeta;
import com.fourmeme.bot.enums.MarketPair;
import com.fourmeme.bot.mapper.BotTaskMapper;
import com.fourmeme.bot.request.CreateTaskRequest;
import com.fourmeme.bot.service.BotTaskService;
import com.fourmeme.bot.service.TokenMetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotTaskServiceImpl implements BotTaskService {

    private final BotTaskMapper botTaskMapper;
    private final TokenMetaService tokenMetaService;

    @Override
    public BotTask createTask(CreateTaskRequest request) {
        // ===== 基础参数校验 =====
        if (request.getTokenAddress() == null || request.getTokenAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("代币地址不能为空");
        }
        if (request.getMinBuyAmount() == null || request.getMaxBuyAmount() == null) {
            throw new IllegalArgumentException("买卖金额不能为空");
        }
        if (request.getMinBuyAmount().compareTo(request.getMaxBuyAmount()) > 0) {
            throw new IllegalArgumentException("最小金额不能大于最大金额");
        }

        String tokenAddress = request.getTokenAddress().trim().toLowerCase();
        log.info("开始创建任务，代币地址: {}", tokenAddress);

        // ===== 从数据库或链上获取元数据 =====
        // 第一步 /token/info 已经调用过 getOrFetch，此处会命中数据库缓存，无 RPC 开销
        TokenMeta metadata = tokenMetaService.getOrFetch(tokenAddress);
        if (metadata == null) {
            log.error("获取代币元数据失败，地址: {}", tokenAddress);
            throw new IllegalStateException("无法获取代币元数据，请检查代币地址是否正确");
        }
        log.info("成功获取代币元数据: tokenName={}, marketPair={}, decimals={}", metadata.getTokenName(), metadata.getMarketPair(), metadata.getDecimals());

        // ===== 市场识别 =====
        String marketPair = metadata.getMarketPair();
        if (marketPair == null || marketPair.trim().isEmpty()) {
            throw new IllegalStateException("无法自动识别代币所属市场（BNB/USDT），请在 Four.meme 上确认");
        }
        marketPair = marketPair.trim().toUpperCase();
        MarketPair market = MarketPair.fromName(marketPair);

        // ===== 查重（同一代币 + 同一市场只能有一个任务）=====
        BotTask existing = botTaskMapper.selectOne(
                new LambdaQueryWrapper<BotTask>()
                        .eq(BotTask::getTokenAddress, tokenAddress)
                        .eq(BotTask::getMarketPair, marketPair));
        if (existing != null) {
            throw new IllegalStateException(
                    "[" + existing.getTokenName() + "/" + market.getName() + "] 的任务已经存在: " + tokenAddress);
        }

        // ===== 构建任务 =====
        BotTask task = new BotTask();
        task.setTokenAddress(tokenAddress);
        task.setTokenName(metadata.getTokenName());
        task.setMarketPair(marketPair);
        task.setMinBuyAmount(request.getMinBuyAmount());
        task.setMaxBuyAmount(request.getMaxBuyAmount());
        task.setMinIntervalSec(request.getMinIntervalSec() != null
                ? request.getMinIntervalSec() : 10);
        task.setMaxIntervalSec(request.getMaxIntervalSec() != null
                ? request.getMaxIntervalSec() : 30);
        task.setBuyWeight(request.getBuyWeight() != null
                ? request.getBuyWeight() : 60);
        task.setMaxTradesPerRound(request.getMaxTradesPerRound() != null
                ? request.getMaxTradesPerRound() : 1);
        task.setGraduationThreshold(request.getGraduationThreshold() != null
                ? request.getGraduationThreshold()
                : market.getDefaultGraduationThreshold());
        task.setIsRunning(0);
        task.setGraduated(0);

        botTaskMapper.insert(task);
        log.info("创建任务成功: id={} address={} token={} market={}",
                task.getId(), task.getTokenAddress(),
                task.getTokenName(), task.getMarketPair());
        return task;
    }

    @Override
    public BotTask getById(Long id) {
        return botTaskMapper.selectById(id);
    }

    @Override
    public List<BotTask> listAll() {
        return botTaskMapper.selectList(null);
    }

    @Override
    public List<BotTask> listRunning() {
        return botTaskMapper.selectList(
                new LambdaQueryWrapper<BotTask>().eq(BotTask::getIsRunning, 1));
    }

    @Override
    public List<BotTask> listRunningNotGraduated() {
        return botTaskMapper.selectList(
                new LambdaQueryWrapper<BotTask>()
                        .eq(BotTask::getIsRunning, 1)
                        .eq(BotTask::getGraduated, 0));
    }

    @Override
    public BotTask startTask(Long id) {
        BotTask task = botTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }
        if (task.getIsRunning() != null && task.getIsRunning() == 1) {
            throw new IllegalStateException("任务已在运行中");
        }
        task.setIsRunning(1);
        botTaskMapper.updateById(task);
        log.info("启动任务: id={}", id);
        return task;
    }

    @Override
    public BotTask stopTask(Long id) {
        BotTask task = botTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }
        task.setIsRunning(0);
        botTaskMapper.updateById(task);
        log.info("停止任务: id={}", id);
        return task;
    }

    @Override
    public void deleteTask(Long id) {
        BotTask task = botTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }
        if (task.getIsRunning() != null && task.getIsRunning() == 1) {
            throw new IllegalStateException("请先停止任务再删除");
        }
        botTaskMapper.deleteById(id);
        log.info("删除任务: id={}", id);
    }

    @Override
    public void markGraduated(Long id) {
        BotTask task = botTaskMapper.selectById(id);
        if (task == null) {
            return;
        }
        task.setGraduated(1);
        botTaskMapper.updateById(task);
        log.info("标记任务已毕业: id={}", id);
    }

    @Override
    public void update(BotTask task) {
        botTaskMapper.updateById(task);
    }
}