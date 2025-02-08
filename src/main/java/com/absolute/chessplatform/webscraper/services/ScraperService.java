package com.absolute.chessplatform.webscraper.services;

import com.absolute.chessplatform.webscraper.entities.ProxyInfo;
import com.absolute.chessplatform.webscraper.entities.RobotsTxtRules;
import com.absolute.chessplatform.webscraper.entities.ScrapeSession;
import crawlercommons.robots.BaseRobotRules;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Service
public class ScraperService {
    private final ImageProcessingService imageProcessingService;
    private final ExecutorService linkExecutor;
    private final ExecutorService imageExecutor;
    private final RobotsTxtService robotsTxtService;
    private final ProxyProvider proxyProvider;
    private ScrapeSession session;

    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final Set<String> visitedImages = ConcurrentHashMap.newKeySet();


    private static final int MAX_RETRY_ATTEMPTS = 4;
    private static final long INITIAL_RETRY_DELAY_MS = 2000;

    public ScraperService(ImageProcessingService imageProcessingService,
                          @Qualifier("linkExecutor") ExecutorService linkExecutor,
                          @Qualifier("imageExecutor") ExecutorService imageExecutor,
                          ProxyProvider proxyProvider,
                          RobotsTxtService robotsTxtService) {
        this.linkExecutor = linkExecutor;
        this.imageExecutor = imageExecutor;
        this.robotsTxtService = robotsTxtService;
        this.imageProcessingService = imageProcessingService;
        this.proxyProvider = proxyProvider;
    }

    public CompletableFuture<Set<String>> startScraping(String url, int depth) throws URISyntaxException {
        long start = System.currentTimeMillis();

        String domain = new URI(url).getHost();
        BaseRobotRules rules = robotsTxtService.getRules(domain);
        long crawlDelay = (rules != null) ? rules.getCrawlDelay() : 0;
        log.info("Crawl-delay: {}", crawlDelay);

        session = new ScrapeSession(domain, depth, rules);
        log.info("Crawl-delay: {}",
                session.getRobotsTxtRules().getCrawlDelay());

        CompletableFuture<Void> crawlingFuture = crawl(url, 0);

        CompletableFuture<Set<String>> resultFuture = crawlingFuture.thenApply(v -> {
            log.info("Scraping completed in {} ms", System.currentTimeMillis() - start);
            return Collections.unmodifiableSet(visited);
        });

        resultFuture.whenComplete((result, throwable) -> {
            linkExecutor.shutdown();
            imageExecutor.shutdown();
        });
        return resultFuture;
    }

    public CompletableFuture<Void> crawl(String url, int depth) {
        if (!shouldProcess(url, depth)) {
            return CompletableFuture.completedFuture(null);
        }
        log.info("Scraping url {}", url);
        long delay = session.getRobotsTxtRules().getCrawlDelay();
        try {
            if(delay != 0) {
                if(delay > 0) {
                    Thread.sleep(delay);
                }
            }
        } catch (InterruptedException e) {
            log.error("Error processing : {}",e.getMessage());
        }
        return CompletableFuture.supplyAsync(() -> fetchDocument(url), linkExecutor)
                .thenCompose(document -> processDocument(document, depth))
                .exceptionally(ex -> {
                    log.error("Error processing URL: {}, error: {}", url, ex.getMessage());
                    return null;
                });
    }

    private CompletableFuture<Void> processDocument(Document doc, int depth) {
        if (doc == null) return CompletableFuture.completedFuture(null);
        Set<String> links = LinkExtractor.extractLinks(doc);
        Set<String> images = LinkExtractor.extractImages(doc);
        List<CompletableFuture<Void>> linkFutures = new ArrayList<>();
        List<CompletableFuture<Void>> imageFutures = new ArrayList<>();
        for (String link : links) {
            linkFutures.add(crawl(link, depth + 1));
        }
        for (String image : images) {
           if(visitedImages.add(image)) {
               CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> imageProcessingService.processImage(image,session.getDomain()), imageExecutor)
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

    private boolean shouldProcess(String url, int depth) {
        if (depth > session.getMaxDepth() || !LinkExtractor.isLinkRefersToDomain(url, session.getDomain())
                || !robotsTxtService.isAllowed(url, session.getRobotsTxtRules())) {
            return false;
        }
        return visited.add(url);
    }

    public Document fetchDocument(String url) {
        int attempts = 0;
        long delay = INITIAL_RETRY_DELAY_MS;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                String userAgent = UserAgentProvider.getRandomUserAgent();
                //ProxyInfo proxy = proxyProvider.getNextProxy();
                ProxyInfo proxy = null;
                Connection connection = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                        .timeout(30_000);

                if (proxy != null) {
                    connection.proxy(proxy.host(), proxy.port());
                }
                return connection.get();
            } catch (org.jsoup.HttpStatusException e) {
                if (e.getStatusCode() == 429 || e.getStatusCode() == 502) {
                    attempts++;
                    log.warn("HTTP 429 отримано для URL: {}. Спроба {}/{}. Затримка {} мс.", url, attempts, MAX_RETRY_ATTEMPTS, delay);
                    sleep(delay);
                    delay *= 2;
                } else {
                    log.warn("HTTP status error fetching URL: {}. Status: {}", url, e.getStatusCode());
                    return null;
                }
            } catch (SocketException e) {
                log.warn(e.getMessage());
                return null;
            } catch (IOException e) {
                log.warn("Failed to get document by URL: {}", url, e);
                return null;
            }
        }
        log.error("Не вдалося отримати документ за URL: {} після {} спроб", url, MAX_RETRY_ATTEMPTS);
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

    }
}
