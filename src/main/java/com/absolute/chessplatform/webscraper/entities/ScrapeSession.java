package com.absolute.chessplatform.webscraper.entities;

import crawlercommons.robots.BaseRobotRules;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@RequiredArgsConstructor
public class ScrapeSession {
    private final String domain;
    private final int maxDepth;
    private final BaseRobotRules robotsTxtRules;
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final Set<String> visitedImages = ConcurrentHashMap.newKeySet();
}
