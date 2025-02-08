package com.absolute.chessplatform.webscraper.services;

import com.absolute.chessplatform.webscraper.entities.RobotsTxtRules;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;

public class RobotsTxtServiceTest {


//    @Test
//    void testGetRulesValidRobotsTxt() throws Exception {
//        String domain = "example.com";
//        String robotsContent = "User-agent: *\nDisallow: /admin\nCrawl-delay: 10";
//
//        Connection connection = Mockito.mock(Connection.class);
//        Connection.Response response = Mockito.mock(Connection.Response.class);
//
//        Mockito.when(connection.userAgent(Mockito.anyString())).thenReturn(connection);
//        Mockito.when(connection.ignoreContentType(Mockito.anyBoolean())).thenReturn(connection);
//        Mockito.when(connection.timeout(Mockito.anyInt())).thenReturn(connection);
//        Mockito.when(connection.execute()).thenReturn(response);
//        Mockito.when(response.statusCode()).thenReturn(200);
//        Mockito.when(response.body()).thenReturn(robotsContent);
//
//        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
//            jsoupMock.when(() -> Jsoup.connect("https://" + domain + "/robots.txt"))
//                    .thenReturn(connection);
//
//            RobotsTxtService service = new RobotsTxtService();
//            RobotsTxtRules rules = service.getRules(domain);
//
//            Assertions.assertTrue(rules.disallowedPaths().contains("/admin"));
//            Assertions.assertEquals(10000L, rules.crawlDelay());
//        }
//    }
//
//    @Test
//    void testGetRulesNotFound() throws Exception {
//        String domain = "example.com";
//
//        Connection connection = Mockito.mock(Connection.class);
//        Connection.Response response = Mockito.mock(Connection.Response.class);
//
//        Mockito.when(connection.userAgent(Mockito.anyString())).thenReturn(connection);
//        Mockito.when(connection.ignoreContentType(Mockito.anyBoolean())).thenReturn(connection);
//        Mockito.when(connection.timeout(Mockito.anyInt())).thenReturn(connection);
//        Mockito.when(connection.execute()).thenReturn(response);
//        Mockito.when(response.statusCode()).thenReturn(404);
//
//        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
//            jsoupMock.when(() -> Jsoup.connect("https://" + domain + "/robots.txt"))
//                    .thenReturn(connection);
//
//            RobotsTxtService service = new RobotsTxtService();
//            RobotsTxtRules rules = service.getRules(domain);
//
//            Assertions.assertTrue(rules.disallowedPaths().isEmpty());
//            Assertions.assertNull(rules.crawlDelay());
//        }
//    }
//
//
//    @Test
//    void testGetRulesException() {
//        String domain = "example.com";
//
//        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
//            jsoupMock.when(() -> Jsoup.connect("https://" + domain + "/robots.txt"))
//                    .thenThrow(new RuntimeException("Connection error"));
//
//            RobotsTxtService service = new RobotsTxtService();
//            RobotsTxtRules rules = service.getRules(domain);
//
//            Assertions.assertTrue(rules.disallowedPaths().isEmpty());
//            Assertions.assertNull(rules.crawlDelay());
//        }
//    }
//
//    @Test
//    void testGetRulesMultipleUserAgents() throws Exception {
//        String domain = "example.com";
//        String robotsContent = "User-agent: Googlebot\n" +
//                "Disallow: /nogoogle/\n\n" +
//                "User-agent: *\n" +
//                "Disallow: /all/\n" +
//                "Crawl-delay: 5";
//
//        Connection connection = Mockito.mock(Connection.class);
//        Connection.Response response = Mockito.mock(Connection.Response.class);
//
//        Mockito.when(connection.userAgent(Mockito.anyString())).thenReturn(connection);
//        Mockito.when(connection.ignoreContentType(Mockito.anyBoolean())).thenReturn(connection);
//        Mockito.when(connection.timeout(Mockito.anyInt())).thenReturn(connection);
//        Mockito.when(connection.execute()).thenReturn(response);
//        Mockito.when(response.statusCode()).thenReturn(200);
//        Mockito.when(response.body()).thenReturn(robotsContent);
//
//        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
//            jsoupMock.when(() -> Jsoup.connect("https://" + domain + "/robots.txt"))
//                    .thenReturn(connection);
//
//            RobotsTxtService service = new RobotsTxtService();
//            RobotsTxtRules rules = service.getRules(domain);
//
//            Assertions.assertTrue(rules.disallowedPaths().contains("/all/"));
//            Assertions.assertFalse(rules.disallowedPaths().contains("/nogoogle/"));
//            Assertions.assertEquals(5000L, rules.crawlDelay());
//        }
//    }
//
//    @Test
//    void testGetRulesInvalidCrawlDelay() throws Exception {
//        String domain = "example.com";
//        String robotsContent = "User-agent: *\nDisallow: /admin\nCrawl-delay: abc";
//
//        Connection connection = Mockito.mock(Connection.class);
//        Connection.Response response = Mockito.mock(Connection.Response.class);
//
//        Mockito.when(connection.userAgent(Mockito.anyString())).thenReturn(connection);
//        Mockito.when(connection.ignoreContentType(Mockito.anyBoolean())).thenReturn(connection);
//        Mockito.when(connection.timeout(Mockito.anyInt())).thenReturn(connection);
//        Mockito.when(connection.execute()).thenReturn(response);
//        Mockito.when(response.statusCode()).thenReturn(200);
//        Mockito.when(response.body()).thenReturn(robotsContent);
//
//        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
//            jsoupMock.when(() -> Jsoup.connect("https://" + domain + "/robots.txt"))
//                    .thenReturn(connection);
//
//            RobotsTxtService service = new RobotsTxtService();
//            RobotsTxtRules rules = service.getRules(domain);
//
//            Assertions.assertTrue(rules.disallowedPaths().contains("/admin"));
//            Assertions.assertNull(rules.crawlDelay());
//        }
//    }
//
//
//    @Test
//    void testGetRulesEmptyDisallow() throws Exception {
//        String domain = "example.com";
//        String robotsContent = "User-agent: *\nDisallow:";
//
//        Connection connection = Mockito.mock(Connection.class);
//        Connection.Response response = Mockito.mock(Connection.Response.class);
//
//        Mockito.when(connection.userAgent(Mockito.anyString())).thenReturn(connection);
//        Mockito.when(connection.ignoreContentType(Mockito.anyBoolean())).thenReturn(connection);
//        Mockito.when(connection.timeout(Mockito.anyInt())).thenReturn(connection);
//        Mockito.when(connection.execute()).thenReturn(response);
//        Mockito.when(response.statusCode()).thenReturn(200);
//        Mockito.when(response.body()).thenReturn(robotsContent);
//
//        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
//            jsoupMock.when(() -> Jsoup.connect("https://" + domain + "/robots.txt"))
//                    .thenReturn(connection);
//
//            RobotsTxtService service = new RobotsTxtService();
//            RobotsTxtRules rules = service.getRules(domain);
//
//            Assertions.assertTrue(rules.disallowedPaths().isEmpty());
//            Assertions.assertNull(rules.crawlDelay());
//        }
//    }
//
//
//    @Test
//    void testIsAllowedDisallowedPath() {
//        RobotsTxtRules rules = new RobotsTxtRules(new HashSet<>(Arrays.asList("/admin")), null);
//        RobotsTxtService service = new RobotsTxtService();
//        boolean allowed = service.isAllowed("https://example.com/admin/settings", rules);
//        Assertions.assertFalse(allowed);
//    }
//
//    @Test
//    void testIsAllowedAllowedPath() {
//
//        RobotsTxtRules rules = new RobotsTxtRules(new HashSet<>(Arrays.asList("/admin")), null);
//        RobotsTxtService service = new RobotsTxtService();
//        boolean allowed = service.isAllowed("https://example.com/user/settings", rules);
//        Assertions.assertTrue(allowed);
//    }
//
//    @Test
//    void testIsAllowedWildcard() {
//
//        RobotsTxtRules rules = new RobotsTxtRules(new HashSet<>(Arrays.asList("/")), null);
//        RobotsTxtService service = new RobotsTxtService();
//        boolean allowed = service.isAllowed("https://example.com/anything", rules);
//        Assertions.assertFalse(allowed);
//    }
//
//    @Test
//    void testIsAllowedInvalidUrl() {
//
//        RobotsTxtRules rules = new RobotsTxtRules(new HashSet<>(Arrays.asList("/admin")), null);
//        RobotsTxtService service = new RobotsTxtService();
//        boolean allowed = service.isAllowed("invalid url", rules);
//        Assertions.assertTrue(allowed);
//    }
}
