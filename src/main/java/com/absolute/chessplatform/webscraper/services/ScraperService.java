package com.absolute.chessplatform.webscraper.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class ScraperService {
    private final Set<String> visited;
    private final ExecutorService executor;
    private final Phaser phaser;
    private int maxDepth;
    private String domain;

    public ScraperService() {
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.visited = ConcurrentHashMap.newKeySet();
        this.phaser = new Phaser(1);
    }

    public Set<String> startScraping(String url, int depth) throws IOException, URISyntaxException {
        domain = new URI(url).getHost();
        maxDepth = depth;
        long start = System.currentTimeMillis();
        crawl(url, 0);
        phaser.arriveAndDeregister();
        phaser.awaitAdvance(phaser.getPhase());
        executor.shutdown();
        long end = System.currentTimeMillis() - start;
        log.info("Scraping completed in {} ms", end);
        return visited;
    }

    public void crawl(String url, int depth) throws IOException, URISyntaxException {
        if (visited.contains(url) || depth > maxDepth || !LinkExtractor.isLinkRefersToDomain(url,domain)) {
            return;
        }
        visited.add(url);
        Document doc = Jsoup.connect(url)
                .timeout(10 * 1000)
                .get();
        Set<String> links = LinkExtractor.extractLinks(doc);
        Set<String> imagesUrl = LinkExtractor.extractImages(doc);
        for (String link : links) {
            phaser.register();
            executor.submit(() -> {
                try {
                    log.info("Scraping url {}", link);
                    crawl(link, depth + 1);
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }  finally {
                    phaser.arriveAndDeregister();
                }
            });
        }

    }
}
