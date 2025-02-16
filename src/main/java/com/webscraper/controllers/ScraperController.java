package com.webscraper.controllers;

import com.webscraper.entities.ScraperBody;
import com.webscraper.services.impl.ScraperServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;


/**
 * REST controller for starting web scraping.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ScraperController {

    private final ScraperServiceImpl scraperService;

    /**
     * Starts the scraping process with the provided parameters.
     *
     * @param scraperBody the body containing URL, recursion depth, delay and proxies
     * @return a CompletableFuture with the response entity containing scraped links or an error message
     * @throws URISyntaxException if the provided URL is invalid
     */
    @PostMapping("/start")
    public CompletableFuture<ResponseEntity<?>> startScraping(@RequestBody ScraperBody scraperBody) throws URISyntaxException {
        if (scraperBody == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("No scraper body found"));
        }

        return scraperService.startScraping(
                        scraperBody.getUrl(),
                        scraperBody.getRecursionDepth(),
                        scraperBody.getRequestDelay(),
                        scraperBody.getProxies()
                )
                .thenApply(result -> {
                    if (result.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No links found");
                    }
                    return ResponseEntity.ok(result);
                })
                .exceptionally(ex -> {
                    // Log the full exception (could be improved by adding a proper logging statement)
                    return ResponseEntity.internalServerError().body("Processing error: " + ex.getMessage());
                });
    }


//    @GetMapping("/images")
//    public ResponseEntity<?> getImagesInfo(@RequestParam String site) {
//
//        var images = scraperService.getImageInfoBySite(site);
//        if (images == null || images.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Зображення не знайдено для сайту " + site);
//        }
//        return ResponseEntity.ok(images);
//    }

}


