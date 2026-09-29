package com.fourmeme.bot.service;

/**
 * 毕业检测器
 * 定时扫描运行中的任务，检测代币是否已从 Four.meme 迁移到 PancakeSwap
 */
public interface GraduationDetector {

    /**
     * 检测所有运行中且未毕业的任务
     * 定时触发，也可以手动调用
     */
    void detectAllTasks();
}