package com.webscraper.services;

import crawlercommons.robots.BaseRobotRules;

/**
 * Service interface for retrieving and checking robots.txt rules for a domain.
 */
public interface RobotsTxtService {

    /**
     * Retrieves the robots.txt rules for the specified domain.
     *
     * @param domain the domain to retrieve rules for
     * @return the parsed {@link BaseRobotRules} or null if rules cannot be retrieved
     */
    BaseRobotRules getRules(String domain);

    /**
     * Determines whether the given URL is allowed to be crawled according to the provided robots.txt rules.
     *
     * @param url   the URL to check
     * @param rules the robots.txt rules to apply
     * @return true if crawling is allowed; false otherwise
     */
    boolean isAllowed(String url, BaseRobotRules rules);
}
