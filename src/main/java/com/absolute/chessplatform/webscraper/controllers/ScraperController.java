package com.absolute.chessplatform.webscraper.controllers;

import com.absolute.chessplatform.webscraper.entities.ScraperBody;
import com.absolute.chessplatform.webscraper.services.ScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ScraperController {

    private final ScraperService scraperService;

    @PostMapping("/start")
    public CompletableFuture<ResponseEntity<?>> startScraping(@RequestBody ScraperBody scraperBody) throws URISyntaxException {
        if (scraperBody == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("No scraper body found"));
        }

        return scraperService.startScraping(scraperBody.getUrl(), scraperBody.getRecursionDepth())
                .thenApply(result -> {
                    if (result.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Не знайдено посилань");
                    }
                    return ResponseEntity.ok(result);
                })
                .exceptionally(ex -> ResponseEntity.internalServerError().body("Помилка обробки: " + ex.getMessage()));
    }
}
