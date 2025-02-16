package com.webscraper.services.impl;

import com.google.common.util.concurrent.RateLimiter;
import com.webscraper.engines.ScraperEngine;
import com.webscraper.entities.ProxyInfo;
import com.webscraper.entities.ScraperSession;
import com.webscraper.services.DocumentService;
import com.webscraper.services.ProxySelectorService;
import com.webscraper.services.RobotsTxtService;
import com.webscraper.services.ScraperService;
import com.webscraper.services.handlers.ContentHandler;
import crawlercommons.robots.BaseRobotRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Service facade for starting and managing the scraping process.
 */
@Slf4j
@Service
public class ScraperServiceImpl implements ScraperService {

    private final ExecutorService linkExecutor;
    private final ExecutorService imageExecutor;
    private final RobotsTxtService robotsTxtService;
    private final ProxySelectorService proxySelectorService;
    private final DocumentService documentService;
    private final List<ContentHandler> contentHandlers;

    public ScraperServiceImpl(@Qualifier("linkExecutor") ExecutorService linkExecutor,
                              @Qualifier("imageExecutor") ExecutorService imageExecutor,
                              RobotsTxtService robotsTxtService,
                              ProxySelectorService proxySelectorService,
                              DocumentService documentService,
                              List<ContentHandler> contentHandlers) {
        this.linkExecutor = linkExecutor;
        this.imageExecutor = imageExecutor;
        this.robotsTxtService = robotsTxtService;
        this.proxySelectorService = proxySelectorService;
        this.documentService = documentService;
        this.contentHandlers = contentHandlers;
    }

    /**
     * Starts the scraping process.
     *
     * @param url        the starting URL
     * @param maxDepth   the maximum recursion depth
     * @param userDelay  a delay (in milliseconds) between requests (if provided)
     * @param userProxies a list of proxies to use
     * @return a CompletableFuture containing the set of visited links
     * @throws URISyntaxException if the URL is invalid
     */
    @Override
    public CompletableFuture<Set<String>> startScraping(String url, int maxDepth, Long userDelay, List<ProxyInfo> userProxies) throws URISyntaxException {
        long startTime = System.currentTimeMillis();

        String domain = new URI(url).getHost();
        BaseRobotRules rules = robotsTxtService.getRules(domain);
        if (rules != null) {
            log.info("Crawl-delay (from robots.txt): {}", rules.getCrawlDelay());
        }
        userProxies = com.webscraper.utils.ProxyCheckerService.filterWorkingProxies(userProxies);
        ScraperSession session = new ScraperSession(url, domain, maxDepth, rules, userDelay, userProxies);

        long delay = determineDelay(session);
        if (delay > 0) {
            double permitsPerSecond = 1000.0 / delay;
            session.setRateLimiter(RateLimiter.create(permitsPerSecond));
            log.info("RateLimiter set with permitsPerSecond: {}", permitsPerSecond);
        }

        ScraperEngine engine = new ScraperEngine(linkExecutor, documentService, robotsTxtService, proxySelectorService, contentHandlers);
        CompletableFuture<Void> crawlingFuture = engine.crawl(session.getUrl(), session, 0);

        CompletableFuture<Set<String>> resultFuture = crawlingFuture.thenApply(v -> Collections.unmodifiableSet(session.getVisitedLinksUrl()));

        resultFuture.whenComplete((result, throwable) -> {
            log.info("Scraping completed in {} ms", System.currentTimeMillis() - startTime);
        });
        return resultFuture;
    }

    /**
     * Determines the delay between requests based on robots.txt or user configuration.
     *
     * @param session the current scraper session
     * @return the delay in milliseconds
     */
    private long determineDelay(ScraperSession session) {
        if (session.getRobotsTxtRules() != null && session.getRobotsTxtRules().getCrawlDelay() > 0) {
            return session.getRobotsTxtRules().getCrawlDelay();
        } else if (session.getUserDelay() != null && session.getUserDelay() > 0) {
            return session.getUserDelay();
        }
        return 0;
    }
}
