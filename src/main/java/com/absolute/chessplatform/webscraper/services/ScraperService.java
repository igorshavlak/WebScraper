package com.absolute.chessplatform.webscraper.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Service
public class ScraperService {
    private final ImageProcessingService imageProcessingService;
    private final Set<String> visited;
    private final ExecutorService executor;
    private final Phaser phaser;
    private final PropertiesLoaderSupport propertiesLoaderSupport;
    private int maxDepth;
    private String domain;

    public ScraperService(ImageProcessingService imageProcessingService, PropertiesLoaderSupport propertiesLoaderSupport) {
        this.imageProcessingService = imageProcessingService;
        this.executor = Executors.newFixedThreadPool(30);
        this.visited = ConcurrentHashMap.newKeySet();
        this.phaser = new Phaser(1);
        this.propertiesLoaderSupport = propertiesLoaderSupport;
    }

    public Set<String> startScraping(String url, int depth) throws IOException, URISyntaxException {
        log.info(String.valueOf(Runtime.getRuntime().availableProcessors()));
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

    public void crawl(String url, int depth) throws URISyntaxException {
        if (depth > maxDepth || !LinkExtractor.isLinkRefersToDomain(url, domain)) {
            return;
        }
        log.info("Scraping url {}", url);
        if (!visited.add(url)) {
            return;
        }
        Document document = fetchDocument(url);
        if (document == null) {
            return;
        }
        Set<String> links = LinkExtractor.extractLinks(document);
        Set<String> imagesUrl = LinkExtractor.extractImages(document);
        for (String link : links) {
            phaser.register();
            executor.submit(() -> {
                try {
                    crawl(link, depth + 1);
                } catch (URISyntaxException e) {
                    log.error(e.getMessage());
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }
        /*
        for (String image : imagesUrl) {
            if (LinkExtractor.isLinkRefersToDomain(image, domain)) {

                try {
                    imageProcessingService.processImage(image);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

         */
    }

    public Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .timeout(20_000)
                    .get();
        } catch (org.jsoup.HttpStatusException e) {
            log.warn("HTTP status error fetching URL: {}. Status: {}", url, e.getStatusCode());
            return null;
        } catch (SocketException e){
            log.warn(e.getMessage());
            return null;
        }
        catch (IOException e) {
            log.warn("Failed to get document by URL: {}", url, e);
            return null;
        }
    }
}
