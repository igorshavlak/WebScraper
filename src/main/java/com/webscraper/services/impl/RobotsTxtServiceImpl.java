package com.webscraper.services.impl;

import com.webscraper.services.RobotsTxtService;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Implementation of {@link RobotsTxtService} for retrieving and parsing the robots.txt file.
 */
@Slf4j
@Service
public class RobotsTxtServiceImpl implements RobotsTxtService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";

    /**
     * Retrieves the robots.txt rules for the given domain.
     *
     * @param domain the domain for which to retrieve robots.txt
     * @return the parsed {@link BaseRobotRules} or null if not available
     */
    @Override
    public BaseRobotRules getRules(String domain) {
        String robotsUrl = "https://" + domain + "/robots.txt";
        byte[] content = downloadRobotsTxt(domain);
        if (content == null) {
            return null;
        }
        SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
        List<String> userAgents = List.of(USER_AGENT);
        return parser.parseContent(robotsUrl, content, "text/plain", userAgents);
    }

    /**
     * Downloads the content of robots.txt for the given domain.
     *
     * @param domain the domain for which to download robots.txt
     * @return a byte array containing the content, or null if not available
     */
    private byte[] downloadRobotsTxt(String domain) {
        String robotsUrl = "https://" + domain + "/robots.txt";
        try {
            Connection.Response response = Jsoup.connect(robotsUrl)
                    .userAgent(USER_AGENT)
                    .ignoreContentType(true)
                    .timeout(10000)
                    .execute();
            if (response.statusCode() != 200) {
                return null;
            }
            return response.body().getBytes(StandardCharsets.UTF_8);
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 404) {
                log.info("robots.txt not found for domain: {}", domain);
                return null;
            }
        } catch (Exception e) {
            log.error("Error downloading robots.txt from URL: {}. Error: {}", robotsUrl, e.getMessage(), e);
            return null;
        }
        return new byte[0];
    }

    /**
     * Checks whether crawling is allowed for the specified URL according to the provided robots.txt rules.
     *
     * @param url   the URL to check
     * @param rules the robots.txt rules to apply; may be null
     * @return true if allowed, false otherwise
     */
    public boolean isAllowed(String url, BaseRobotRules rules) {
        if (rules == null) {
            return true;
        }
        return rules.isAllowed(url);
    }
}
