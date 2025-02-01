package com.absolute.chessplatform.webscraper.controllers;

import com.absolute.chessplatform.webscraper.entities.ScraperBody;
import com.absolute.chessplatform.webscraper.services.ScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ScraperController {

    private final ScraperService scraperService;

    @GetMapping("/start")
    private ResponseEntity<?> startScraping(@RequestBody ScraperBody scraperBody) throws IOException, URISyntaxException {
        if(scraperBody == null) {
            return ResponseEntity.badRequest().body("No scraper body found");
        }
        Set<String> links = scraperService.startScraping(scraperBody.getUrl(),scraperBody.getRecursionDepth());
        if(links.isEmpty()) {
            return ResponseEntity.badRequest().body("No links found");
        }
        return ResponseEntity.ok(links);
    }
}