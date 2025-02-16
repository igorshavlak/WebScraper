package com.webscraper.services.handlers;

import com.webscraper.entities.ScraperSession;
import com.webscraper.utils.LinkExtractor;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Handles processing of links found in a web page.
 * <p>
 * Extracts hyperlinks from the document and delegates crawling to the provided {@link LinkCrawler}.
 */
@RequiredArgsConstructor
@Component
public class LinkHandler implements ContentHandler {

    private final LinkCrawler linkCrawler;

    /**
     * Processes the document to extract links and initiates crawling for each extracted link.
     *
     * @param document     the JSoup Document to process
     * @param session      the current scraping session
     * @param currentDepth the current recursion depth
     * @return a CompletableFuture that completes when all link crawling tasks are finished
     */
    @Override
    public CompletableFuture<Void> process(Document document, ScraperSession session, int currentDepth) {
        Set<String> links = LinkExtractor.extractLinks(document);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String link : links) {
            futures.add(linkCrawler.crawl(link, session, currentDepth + 1));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
