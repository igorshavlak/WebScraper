package com.webscraper.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class URLUtilsTest {


    @Test
    void testNormalizeUrl_Null() {
        assertNull(URLUtils.normalizeUrl(null), "При передачі null має повертатися null");
    }

    @Test
    void testNormalizeUrl_EmptyString() {
        assertNull(URLUtils.normalizeUrl("    "), "При передачі пустого рядка має повертатися null");
    }

    @Test
    void testNormalizeUrl_TrimAndLowerCase() {
        String input = " HTTP://EXAMPLE.COM ";
        String expected = "http://example.com";
        assertEquals(expected, URLUtils.normalizeUrl(input));
    }

    @Test
    void testNormalizeUrl_RemoveFragment() {
        String input = "http://example.com/page#section";
        String expected = "http://example.com/page";
        assertEquals(expected, URLUtils.normalizeUrl(input));
    }

    @Test
    void testNormalizeUrl_RemoveDefaultPortForHttp() {
        String input = "http://example.com:80/page";
        String expected = "http://example.com/page";
        assertEquals(expected, URLUtils.normalizeUrl(input));
    }

    @Test
    void testNormalizeUrl_RemoveDefaultPortForHttps() {
        String input = "https://example.com:443/page";
        String expected = "https://example.com/page";
        assertEquals(expected, URLUtils.normalizeUrl(input));
    }

    @Test
    void testNormalizeUrl_KeepNonDefaultPort() {
        String input = "http://example.com:8080/page";
        String expected = "http://example.com:8080/page";
        assertEquals(expected, URLUtils.normalizeUrl(input));
    }

    @Test
    void testNormalizeUrl_InvalidUrl() {
        String input = "ht@tp://invalid-url";
        assertNull(URLUtils.normalizeUrl(input), "Некоректний URL має повертати null");
    }


    @Test
    void testIsSameDomain_DirectMatch() {
        assertTrue(URLUtils.isSameDomain("http://example.com", "example.com"),
                "Домен повинен співпадати");
    }

    @Test
    void testIsSameDomain_WwwMatch() {
        assertTrue(URLUtils.isSameDomain("http://www.example.com", "example.com"),
                "Домен www.example.com має вважатися співпадаючим з example.com");
    }

    @Test
    void testIsSameDomain_SubdomainMatch() {
        assertTrue(URLUtils.isSameDomain("http://sub.example.com", "example.com"),
                "Піддомен має вважатися співпадаючим, якщо закінчується на .example.com");
    }

    @Test
    void testIsSameDomain_NonMatching() {
        assertFalse(URLUtils.isSameDomain("http://example.org", "example.com"),
                "Різні домени не повинні співпадати");
    }

    @Test
    void testIsSameDomain_InvalidUrl() {
        assertFalse(URLUtils.isSameDomain("not a url", "example.com"),
                "Некоректний URL має повертати false");
    }

    @Test
    void testIsSameDomain_CaseInsensitive() {
        assertTrue(URLUtils.isSameDomain("http://EXAMPLE.COM", "example.com"),
                "Порівняння має бути без врахування регістру");
        assertTrue(URLUtils.isSameDomain("http://WWW.EXAMPLE.COM", "example.com"),
                "Порівняння має бути без врахування регістру");
        assertTrue(URLUtils.isSameDomain("http://Sub.Example.Com", "example.com"),
                "Порівняння має бути без врахування регістру");
    }
}
