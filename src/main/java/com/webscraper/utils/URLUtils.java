package com.webscraper.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.net.URIBuilder;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class for URL normalization and domain checking.
 */
@Slf4j
public class URLUtils {

    /**
     * Normalizes the provided URL by ensuring a lowercase scheme and host, removing fragments,
     * and omitting default ports.
     *
     * @param url the URL to normalize
     * @return the normalized URL as a String, or null if the URL is invalid or empty
     */
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
            log.warn("Invalid URL: {}. Skipping processing.", url);
            return null;
        }
    }

    /**
     * Checks if the given URL belongs to the specified domain.
     *
     * @param url    the URL to check
     * @param domain the domain to compare against
     * @return true if the URL is from the same domain; false otherwise
     */
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
