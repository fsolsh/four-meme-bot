package com.fourmeme.bot.service.impl;

import com.fourmeme.bot.entity.BotTask;
import com.fourmeme.bot.entity.TradeRecord;
import com.fourmeme.bot.entity.Wallet;
import com.fourmeme.bot.service.BotScheduler;
import com.fourmeme.bot.service.BotTaskService;
import com.fourmeme.bot.service.TradeRecordService;
import com.fourmeme.bot.service.TradeService;
import com.fourmeme.bot.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotSchedulerImpl implements BotScheduler {

    private final BotTaskService botTaskService;
    private final TradeRecordService tradeRecordService;
    private final WalletService walletService;
    private final TradeService tradeService;

    private final Random random = new Random();

    /** 记录每个钱包上次被使用的时间，避免同一钱包过于频繁交易 */
    private final ConcurrentHashMap<Long, Long> walletCooldown = new ConcurrentHashMap<>();

    /** 同一钱包的最小冷却时间（毫秒） */
    private static final long WALLET_MIN_COOLDOWN_MS = 10_000L;

    /**
     * 主调度循环：每 5 秒检查一次运行中的任务
     */
    @Override
    @Scheduled(fixedDelay = 5000)
    public void scheduleTasks() {
        List<BotTask> runningTasks = botTaskService.listRunning();

        for (BotTask task : runningTasks) {
            try {
                executeRound(task);
            } catch (Exception e) {
                log.error("执行任务轮次失败 taskId={}: {}", task.getId(), e.getMessage());
            }
        }
    }

    @Override
    public void executeRound(BotTask task) throws Exception {
        List<Wallet> wallets = walletService.getAllWallets(1);
        if (wallets.isEmpty()) {
            log.warn("没有可用的钱包，跳过任务 {}", task.getId());
            return;
        }

        int tradesPerRound = task.getMaxTradesPerRound() != null ? task.getMaxTradesPerRound() : 1;

        for (int i = 0; i < tradesPerRound; i++) {
            Wallet wallet = selectWallet(wallets);
            if (wallet == null) {
                log.debug("所有钱包都在冷却中，跳过本轮");
                break;
            }

            boolean isBuy = random.nextInt(100) < task.getBuyWeight();
            BigDecimal amount = randomBetween(
                    task.getMinBuyAmount(), task.getMaxBuyAmount());
            BigInteger gasPrice = tradeService.getRandomGasPrice();

            String tradeType = isBuy ? "BUY" : "SELL";
            String stage = task.getGraduated() == 0 ? "BONDING_CURVE" : "PANCAKE";

            TradeRecord record = new TradeRecord();
            record.setWalletId(wallet.getId());
            record.setTokenAddress(task.getTokenAddress());
            record.setTokenName(task.getTokenName());
            record.setQuoteName(task.getMarketPair());
            record.setTradeType(tradeType);
            record.setQuoteAmount(isBuy ? amount : BigDecimal.ZERO);
            record.setTokenAmount(isBuy ? BigDecimal.ZERO : amount);
            record.setGasPrice(gasPrice.longValue());
            record.setStage(stage);
            record.setStatus(0);

            try {
                String txHash = tradeService.executeTrade(wallet, task, isBuy, amount, gasPrice);
                record.setTxHash(txHash);
                record.setStatus(1);
                log.info("交易成功 [{}] wallet={} token={} amount={} txHash={}",
                        tradeType, wallet.getAddress(),
                        task.getTokenAddress(), amount, txHash);

                walletCooldown.put(wallet.getId(), System.currentTimeMillis());

            } catch (Exception e) {
                record.setStatus(2);
                record.setErrorMsg(e.getMessage());
                log.error("交易失败 [{}] wallet={} token={}: {}",
                        tradeType, wallet.getAddress(),
                        task.getTokenAddress(), e.getMessage());
            }

            tradeRecordService.save(record);

            int interval = randomBetweenInt(
                    task.getMinIntervalSec(), task.getMaxIntervalSec());
            if (interval > 0 && i < tradesPerRound - 1) {
                Thread.sleep(interval * 1000L);
            }
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 选择钱包：排除最近被使用过的钱包
     */
    private Wallet selectWallet(List<Wallet> wallets) {
        long now = System.currentTimeMillis();

        List<Wallet> available = wallets.stream()
                .filter(w -> {
                    Long lastUsed = walletCooldown.get(w.getId());
                    return lastUsed == null || (now - lastUsed) > WALLET_MIN_COOLDOWN_MS;
                })
                .collect(Collectors.toList());

        if (available.isEmpty()) return null;
        return available.get(random.nextInt(available.size()));
    }

    private BigDecimal randomBetween(BigDecimal min, BigDecimal max) {
        double range = max.subtract(min).doubleValue();
        double value = min.doubleValue() + random.nextDouble() * range;
        return BigDecimal.valueOf(value).setScale(18, RoundingMode.DOWN);
    }

    private int randomBetweenInt(int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }
}