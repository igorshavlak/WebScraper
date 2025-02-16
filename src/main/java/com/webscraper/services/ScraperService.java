package com.webscraper.services;

import com.webscraper.entities.ProxyInfo;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ScraperService {
    CompletableFuture<Set<String>> startScraping(String url, int maxDepth, Long userDelay, List<ProxyInfo> userProxies) throws URISyntaxException;
}
