package com.webscraper.services.handlers;

import com.webscraper.entities.ScraperSession;
import org.jsoup.nodes.Document;
import java.util.concurrent.CompletableFuture;

/**
 * Defines the contract for processing a document during web scraping.
 */
public interface ContentHandler {

    /**
     * Processes the provided document within the given scraping session at a specified recursion depth.
     *
     * @param document     the JSoup Document to process
     * @param session      the current scraping session
     * @param currentDepth the current recursion depth
     * @return a CompletableFuture that completes when processing is finished
     */
    CompletableFuture<Void> process(Document document, ScraperSession session, int currentDepth);
}
