package com.webscraper.entities;

import java.util.Set;

/**
 * Record representing the rules parsed from a robots.txt file.
 *
 * @param disallowedPaths a set of paths disallowed for crawling
 * @param crawlDelay      the delay (in milliseconds) between consecutive requests as specified in robots.txt
 */
public record RobotsTxtRules(Set<String> disallowedPaths, Long crawlDelay) {
}
