package com.webscraper.controllers;

import com.webscraper.entities.ScraperBody;
import com.webscraper.services.ScraperService;
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

        return scraperService.startScraping(
                        scraperBody.getUrl(),
                        scraperBody.getRecursionDepth(),
                        scraperBody.getRequestDelay(),
                        scraperBody.getProxies()
                )
                .thenApply(result -> {
                    if (result.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Не знайдено посилань");
                    }
                    return ResponseEntity.ok(result);
                })
                .exceptionally(ex -> ResponseEntity.internalServerError().body("Помилка обробки: " + ex.getMessage()));
    }
//
//    // Новий endpoint для отримання інформації про зображення для певного сайту
//    @GetMapping("/images")
//    public ResponseEntity<?> getImagesInfo(@RequestParam String site) {
//        // Припускаємо, що imageProcessingService має метод getImageInfoBySite()
//        var images = scraperService.getImageInfoBySite(site);
//        if (images == null || images.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Зображення не знайдено для сайту " + site);
//        }
//        return ResponseEntity.ok(images);
//    }
}
