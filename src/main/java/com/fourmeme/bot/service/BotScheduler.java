package com.fourmeme.bot.service;

import com.fourmeme.bot.entity.BotTask;

/**
 * Volume Bot 主调度器
 * 定时扫描运行中的任务并执行交易
 */
public interface BotScheduler {

    /**
     * 主调度循环：每 5 秒扫描一次运行中的任务
     */
    void scheduleTasks();

    /**
     * 对单个任务执行一轮交易
     *
     * @param task 任务配置
     */
    void executeRound(BotTask task) throws Exception;
}