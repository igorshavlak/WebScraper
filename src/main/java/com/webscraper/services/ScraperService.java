package com.webscraper.services;

import com.webscraper.entities.ProxyInfo;
import com.webscraper.entities.ScraperSession;
import com.webscraper.providers.ProxyProvider;
import com.webscraper.utils.LinkExtractor;
import com.webscraper.utils.ProxyCheckerService;
import com.webscraper.utils.URLUtils;
import crawlercommons.robots.BaseRobotRules;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.RateLimiter;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ScraperService {
    private final ImageProcessingService imageProcessingService;
    private final ExecutorService linkExecutor;
    private final ExecutorService imageExecutor;
    private final RobotsTxtService robotsTxtService;
    private final ProxySelectorService proxySelectorService;
    private final DocumentService documentService;

    public ScraperService(ImageProcessingService imageProcessingService,
                          @Qualifier("linkExecutor") ExecutorService linkExecutor,
                          @Qualifier("imageExecutor") ExecutorService imageExecutor,
                          ProxySelectorService proxySelectorService,
                          RobotsTxtService robotsTxtService,
                          DocumentService documentService) {
        this.linkExecutor = linkExecutor;
        this.imageExecutor = imageExecutor;
        this.robotsTxtService = robotsTxtService;
        this.imageProcessingService = imageProcessingService;
        this.proxySelectorService = proxySelectorService;
        this.documentService = documentService;
    }

    public CompletableFuture<Set<String>> startScraping(String url, int maxDepth, Long userDelay, List<ProxyInfo> userProxies) throws URISyntaxException {
        long start = System.currentTimeMillis();

        String domain = new URI(url).getHost();
        BaseRobotRules rules = robotsTxtService.getRules(domain);
        if (rules != null) {
            long crawlDelay = rules.getCrawlDelay();
            log.info("Crawl-delay (з Robots.txt): {}", crawlDelay);
            log.info("Crawl-delay (з сесії): {}", rules.getCrawlDelay());
        }
        userProxies = ProxyCheckerService.filterWorkingProxies(userProxies);
        ScraperSession session = new ScraperSession(url, domain, maxDepth, rules, userDelay, userProxies);

        CompletableFuture<Void> crawlingFuture = crawl(session.getUrl(), session, 0);

        CompletableFuture<Set<String>> resultFuture = crawlingFuture.thenApply(v -> Collections.unmodifiableSet(session.getVisited()));

        resultFuture.whenComplete((result, throwable) -> {
            log.info("Скрапінг завершено за {} мс", System.currentTimeMillis() - start);
        });
        return resultFuture;
    }

    public CompletableFuture<Void> crawl(String url, ScraperSession session, int currentDepth) {
        String normalizedUrl = URLUtils.normalizeUrl(url);
        if (normalizedUrl == null || !shouldProcess(normalizedUrl, session, currentDepth)) {
            return CompletableFuture.completedFuture(null);
        }
        log.info("Скрапінг URL: {} на глибині {}", normalizedUrl, currentDepth);

        long sleepTime = 0;
        if (session.getRobotsTxtRules() != null && session.getRobotsTxtRules().getCrawlDelay() > 0) {
            sleepTime = session.getRobotsTxtRules().getCrawlDelay();
        } else if (session.getUserDelay() != null && session.getUserDelay() > 0) {
            sleepTime = session.getUserDelay();
        }


        if (sleepTime > 0) {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                log.error("Помилка при затримці: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        return CompletableFuture.supplyAsync(() -> {
                    try {
                        return documentService.fetchDocument(normalizedUrl, proxySelectorService.selectProxy(session.getUserProxies()));
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }, linkExecutor)
                .thenCompose(document -> processDocument(document, session, currentDepth))
                .exceptionally(ex -> {
                    log.error("Помилка обробки URL: {}. Помилка: {}", normalizedUrl, ex.getMessage());
                    return null;
                });
    }


    private CompletableFuture<Void> processDocument(Document doc, ScraperSession session, int currentDepth) {
        if (doc == null) return CompletableFuture.completedFuture(null);

        Set<String> links = LinkExtractor.extractLinks(doc);
        Set<String> images = new HashSet<>();
        images.addAll(LinkExtractor.extractImages(doc));
        images.addAll(LinkExtractor.extractCssImages(doc));
        images.addAll(LinkExtractor.extractAnchorImageLinks(doc));

        List<CompletableFuture<Void>> linkFutures = new ArrayList<>();
        List<CompletableFuture<Void>> imageFutures = new ArrayList<>();

        for (String link : links) {
            linkFutures.add(crawl(link, session, currentDepth + 1));
        }
        for (String image : images) {
            if (session.getVisited().add(image)) {
                CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() ->
                                imageProcessingService.processImage(image, session.getDomain()), imageExecutor)
                        .exceptionally(ex -> {
                            log.error("Помилка обробки зображення {}: {}", image, ex.getMessage());
                            return null;
                        });
                imageFutures.add(imageFuture);
            }
        }
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();
        allFutures.addAll(linkFutures);
        allFutures.addAll(imageFutures);
        return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));
    }


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
        return session.getVisited().add(url);
    }

}