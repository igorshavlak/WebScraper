package com.webscraper.entities;

import com.google.common.util.concurrent.RateLimiter;
import crawlercommons.robots.BaseRobotRules;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Represents the state of a scraping session, including visited URLs, proxy settings, and crawling limits.
 */
@Getter
@Setter
public class ScraperSession {

    private String url;
    private String domain;
    private int maxDepth;

    private BaseRobotRules robotsTxtRules;
    private RateLimiter rateLimiter;

    private Set<String> visitedLinksUrl = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Set<String> visitedImagesUrl = Collections.newSetFromMap(new ConcurrentHashMap<>());


    private Long userDelay;
    private List<ProxyInfo> userProxies;

    public ScraperSession(String url, String domain, int maxDepth, BaseRobotRules robotsTxtRules, Long userDelay, List<ProxyInfo> userProxies) {
        this.url = url;
        this.domain = domain;
        this.maxDepth = maxDepth;
        this.robotsTxtRules = robotsTxtRules;
        this.userDelay = userDelay;
        this.userProxies = userProxies;
    }
}
