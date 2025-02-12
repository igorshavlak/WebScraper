package com.webscraper.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class URLUtils {

    public static String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        try {
            URIBuilder uriBuilder = new URIBuilder(url.trim());
            String scheme = uriBuilder.getScheme() != null ? uriBuilder.getScheme().toLowerCase() : "http";
            uriBuilder.setScheme(scheme);
            if (uriBuilder.getHost() != null) {
                uriBuilder.setHost(uriBuilder.getHost().toLowerCase());
            }
            uriBuilder.setFragment(null);

            if ("http".equals(scheme) && uriBuilder.getPort() == 80) {
                uriBuilder.setPort(-1);
            }
            if ("https".equals(scheme) && uriBuilder.getPort() == 443) {
                uriBuilder.setPort(-1);
            }

            URI normalizedUri = uriBuilder.build().normalize();
            return normalizedUri.toString();
        } catch (URISyntaxException e) {
            log.warn("Некоректний URL: {}. Пропускаємо обробку.", url);
            return null;
        }
    }
    public static boolean isSameDomain(String url, String domain) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            host = host.toLowerCase();
            domain = domain.toLowerCase();
            return host.equals(domain) || host.equals("www." + domain) || host.endsWith("." + domain);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
