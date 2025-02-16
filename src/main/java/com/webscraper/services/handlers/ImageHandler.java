package com.webscraper.services.handlers;

import com.webscraper.entities.ScraperSession;
import com.webscraper.services.ImageProcessingService;
import com.webscraper.utils.LinkExtractor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Handles processing of images found in a web page.
 * <p>
 * Extracts images from the document and processes them asynchronously.
 */
@Slf4j
@Component
public class ImageHandler implements ContentHandler {

    private final ImageProcessingService imageProcessingService;
    private final ExecutorService imageExecutor;

    /**
     * Constructs an ImageHandler with the specified image processing service and executor.
     *
     * @param imageProcessingService service for processing images
     * @param imageExecutor          executor for asynchronous image processing tasks
     */
    @Autowired
    public ImageHandler(ImageProcessingService imageProcessingService,
                        @Qualifier("imageExecutor") ExecutorService imageExecutor) {
        this.imageProcessingService = imageProcessingService;
        this.imageExecutor = imageExecutor;
    }

    /**
     * Processes a document to extract and handle image links.
     *
     * @param document     the JSoup Document to process
     * @param session      the current scraping session
     * @param currentDepth the current recursion depth
     * @return a CompletableFuture that completes when all image processing tasks are finished
     */
    @Override
    public CompletableFuture<Void> process(Document document, ScraperSession session, int currentDepth) {
        Set<String> images = new HashSet<>();
        images.addAll(LinkExtractor.extractImages(document));
        images.addAll(LinkExtractor.extractCssImages(document));
        images.addAll(LinkExtractor.extractAnchorImageLinks(document));

        Set<String> visitedImages = session.getVisitedImagesUrl();

        return CompletableFuture.allOf(
                images.stream()
                        .filter(visitedImages::add)
                        .map(image -> CompletableFuture.runAsync(
                                () -> imageProcessingService.processImage(image, session.getDomain()),
                                imageExecutor
                        ).exceptionally(ex -> {
                            log.error("Error processing image {}: {}", image, ex.getMessage());
                            return null;
                        }))
                        .toArray(CompletableFuture[]::new)
        );
    }
}
