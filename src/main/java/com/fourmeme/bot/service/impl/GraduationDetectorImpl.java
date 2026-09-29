package com.fourmeme.bot.service.impl;

import com.fourmeme.bot.entity.BotTask;
import com.fourmeme.bot.service.BotTaskService;
import com.fourmeme.bot.service.FourMemeApiService;
import com.fourmeme.bot.service.GraduationDetector;
import com.fourmeme.bot.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraduationDetectorImpl implements GraduationDetector {

    private final BotTaskService botTaskService;
    private final TradeService tradeService;
    private final FourMemeApiService fourMemeApiService;

    /**
     * 每 30 秒检测一次（间隔由配置 bot.graduation.check-interval-ms 决定）
     * @Scheduled 必须放在实现类的方法上
     */
    @Override
    @Scheduled(fixedDelayString = "${bot.graduation.check-interval-ms}")
    public void detectAllTasks() {
        List<BotTask> runningTasks = botTaskService.listRunningNotGraduated();
        if (runningTasks.isEmpty()) {
            return;
        }

        for (BotTask task : runningTasks) {
            try {
                boolean graduated = false;

                // 方式一：查询 PancakeSwap Factory（最可靠）
                if (tradeService.isTokenOnPancakeSwap(task)) {
                    graduated = true;
                    log.info("代币 {} 已毕业（PancakeSwap交易对已创建, market={}）",
                            task.getTokenAddress(), task.getMarketPair());
                }

                // 方式二：通过 Four.meme 官方 API 查询（补充）
                if (!graduated) {
                    graduated = fourMemeApiService.isGraduatedViaApi(task.getTokenAddress());
                    if (graduated) {
                        log.info("代币 {} 已毕业（Four.meme API返回graduated=true）",
                                task.getTokenAddress());
                    }
                }

                if (graduated) {
                    botTaskService.markGraduated(task.getId());
                    log.info("任务 {} 已标记为毕业，后续交易将自动切换至PancakeSwap",
                            task.getId());
                }
            } catch (Exception e) {
                log.error("检测毕业状态失败 token={}: {}",
                        task.getTokenAddress(), e.getMessage());
            }
        }
    }
}