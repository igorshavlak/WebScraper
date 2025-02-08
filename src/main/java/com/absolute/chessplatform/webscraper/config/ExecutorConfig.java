package com.absolute.chessplatform.webscraper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean
    public ExecutorService linkExecutor(@Value("${crawler.linkPoolSize}") int poolSize) {
        return Executors.newFixedThreadPool(poolSize);
    }
    @Bean
    public ExecutorService imageExecutor(@Value("${crawler.imagePoolSize}") int poolSize) {
        return Executors.newFixedThreadPool(poolSize);
    }
}
