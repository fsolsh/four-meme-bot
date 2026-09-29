package com.fourmeme.bot;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
@MapperScan("com.fourmeme.bot.mapper")
public class FourMemeAppRun {
    public static void main(String[] args) {
        SpringApplication.run(FourMemeAppRun.class, args);
        log.info("four-meme bot is running...");
    }
}