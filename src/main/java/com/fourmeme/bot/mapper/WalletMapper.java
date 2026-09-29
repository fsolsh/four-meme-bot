package com.fourmeme.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fourmeme.bot.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {
}