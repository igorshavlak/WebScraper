package com.webscraper.config;


import com.webscraper.engines.ScraperEngine;
import com.webscraper.services.handlers.LinkCrawler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class ScraperConfig {

    /**
     * Передаємо лямбду, яка делегує виклик методу crawl() із ScraperService.
     * Використовуємо @Lazy, щоб уникнути циклічної залежності.
     */
    @Bean
    public LinkCrawler linkCrawler(@Lazy ScraperEngine scraperEngine) {
        return (url, session, currentDepth) -> scraperEngine.crawl(url, session, currentDepth);
    }
}
