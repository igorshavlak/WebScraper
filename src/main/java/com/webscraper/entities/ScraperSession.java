package com.webscraper.entities;

import crawlercommons.robots.BaseRobotRules;
import lombok.Getter;
import lombok.Setter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class ScraperSession {
    private String url;
    private String domain;
    private int maxDepth;
    private BaseRobotRules robotsTxtRules;
    private Set<String> visited = new HashSet<>();

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
