package com.webscraper.services.handlers;

import com.webscraper.entities.ScraperSession;
import java.util.concurrent.CompletableFuture;

/**
 * Functional interface defining a contract for crawling a link.
 */
@FunctionalInterface
public interface LinkCrawler {

    /**
     * Crawls the specified URL within the given scraping session at a specified recursion depth.
     *
     * @param url          the URL to crawl
     * @param session      the current scraping session
     * @param currentDepth the current recursion depth
     * @return a CompletableFuture that completes when the crawling is finished
     */
    CompletableFuture<Void> crawl(String url, ScraperSession session, int currentDepth);
}
