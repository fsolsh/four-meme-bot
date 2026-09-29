package com.fourmeme.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fourmeme.bot.entity.BotTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BotTaskMapper extends BaseMapper<BotTask> {
}