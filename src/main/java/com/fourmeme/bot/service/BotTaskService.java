package com.fourmeme.bot.service;

import com.fourmeme.bot.entity.BotTask;
import com.fourmeme.bot.request.CreateTaskRequest;

import java.util.List;

public interface BotTaskService {

    /**
     * 创建任务
     */
    BotTask createTask(CreateTaskRequest request);

    /**
     * 按ID查询任务
     */
    BotTask getById(Long id);

    /**
     * 查询所有任务
     */
    List<BotTask> listAll();

    /**
     * 查询运行中的任务
     */
    List<BotTask> listRunning();

    /**
     * 查询运行中且未毕业的任务
     */
    List<BotTask> listRunningNotGraduated();

    /**
     * 启动任务
     */
    BotTask startTask(Long id);

    /**
     * 停止任务
     */
    BotTask stopTask(Long id);

    /**
     * 删除任务
     */
    void deleteTask(Long id);

    /**
     * 标记任务为已毕业
     */
    void markGraduated(Long id);

    /**
     * 更新任务
     */
    void update(BotTask task);
}