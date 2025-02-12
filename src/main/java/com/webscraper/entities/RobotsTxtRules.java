package com.webscraper.entities;


import java.util.Set;


public record RobotsTxtRules(Set<String> disallowedPaths, Long crawlDelay) {
}
