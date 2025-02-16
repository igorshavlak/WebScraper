package com.webscraper.engines;

import com.webscraper.entities.ProxyInfo;
import com.webscraper.entities.ScraperSession;
import com.webscraper.services.DocumentService;
import com.webscraper.services.ProxySelectorService;
import com.webscraper.services.RobotsTxtService;
import com.webscraper.services.handlers.ContentHandler;
import com.webscraper.utils.URLUtils;
import crawlercommons.robots.BaseRobotRules;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * Core engine for crawling and processing webpages.
 */
@Component
@Scope("prototype")
@Slf4j
public class ScraperEngine {

    private final ExecutorService linkExecutor;
    private final DocumentService documentService;
    private final RobotsTxtService robotsTxtService;
    private final ProxySelectorService proxySelectorService;
    private final List<ContentHandler> contentHandlers;

    /**
     * Constructs a new ScraperEngine.
     *
     * @param linkExecutor         the executor for link crawling tasks
     * @param documentService      the service to fetch documents
     * @param robotsTxtService     the service to handle robots.txt rules
     * @param proxySelectorService the service to select proxies
     * @param contentHandlers      the list of content handlers for processing documents
     */
    public ScraperEngine(ExecutorService linkExecutor,
                         DocumentService documentService,
                         RobotsTxtService robotsTxtService,
                         ProxySelectorService proxySelectorService,
                         List<ContentHandler> contentHandlers) {
        this.linkExecutor = linkExecutor;
        this.documentService = documentService;
        this.robotsTxtService = robotsTxtService;
        this.proxySelectorService = proxySelectorService;
        this.contentHandlers = contentHandlers;
    }

    /**
     * Crawls the provided URL.
     *
     * @param url          the URL to crawl
     * @param session      the scraper session
     * @param currentDepth the current recursion depth
     * @return a CompletableFuture that completes when crawling is done
     */
    public CompletableFuture<Void> crawl(String url, ScraperSession session, int currentDepth) {
        String normalizedUrl = URLUtils.normalizeUrl(url);
        if (!shouldProcess(normalizedUrl, session, currentDepth)) {
            return CompletableFuture.completedFuture(null);
        }
        log.info("Crawling URL: {} at depth {}", normalizedUrl, currentDepth);

        if (session.getRateLimiter() != null) {
            session.getRateLimiter().acquire();
        }

        return CompletableFuture.supplyAsync(() -> {
                    try {
                        ProxyInfo proxy = proxySelectorService.selectProxy(session.getUserProxies());
                        return documentService.fetchDocument(normalizedUrl, proxy);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }, linkExecutor)
                .thenCompose(document -> processDocument(document, session, currentDepth))
                .exceptionally(ex -> {
                    log.error("Error processing URL: {}. Error: {}", normalizedUrl, ex.getMessage());
                    return null;
                });
    }

    /**
     * Processes a fetched document using the registered content handlers (LinkHandler, ImageHandler).
     *
     * @param doc          the fetched document
     * @param session      the scraper session
     * @param currentDepth the current recursion depth
     * @return a CompletableFuture that completes when all handlers have finished processing
     */
    private CompletableFuture<Void> processDocument(Document doc, ScraperSession session, int currentDepth) {
        if (doc == null) return CompletableFuture.completedFuture(null);

        List<CompletableFuture<Void>> handlerFutures = new ArrayList<>();
        for (ContentHandler handler : contentHandlers) {
            handlerFutures.add(handler.process(doc, session, currentDepth));
        }
        return CompletableFuture.allOf(handlerFutures.toArray(new CompletableFuture[0]));
    }

    /**
     * Checks whether the URL should be processed based on depth, domain, robots.txt rules, and whether it was already visited.
     *
     * @param url          the URL to check
     * @param session      the scraper session
     * @param currentDepth the current recursion depth
     * @return true if the URL should be processed; false otherwise
     */
    private boolean shouldProcess(String url, ScraperSession session, int currentDepth) {
        if (currentDepth > session.getMaxDepth()) {
            return false;
        }
        if (!URLUtils.isSameDomain(url, session.getDomain())) {
            return false;
        }
        if (!robotsTxtService.isAllowed(url, session.getRobotsTxtRules())) {
            return false;
        }
        return session.getVisitedLinksUrl().add(url);
    }
}
