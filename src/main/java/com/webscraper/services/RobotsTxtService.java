package com.webscraper.services;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class RobotsTxtService {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";

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
                log.info("Сторінки robots.txt не знайдено");
                return null;
            }
        } catch (Exception e) {
            log.error("Помилка завантаження robots.txt з URL: {}", robotsUrl, e);
            return null;
        }
        return new byte[0];
    }

    public boolean isAllowed(String url, BaseRobotRules rules) {
        if (rules == null) {
            return true;
        }
        return rules.isAllowed(url);
    }
}
