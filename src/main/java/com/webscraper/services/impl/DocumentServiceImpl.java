package com.webscraper.services.impl;

import com.webscraper.entities.ProxyInfo;
import com.webscraper.exceptions.NonRetryableException;
import com.webscraper.providers.UserAgentProvider;
import com.webscraper.services.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.SocketException;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {

    @Retryable(
            value = { IOException.class, SocketException.class, HttpStatusException.class },
            maxAttempts = 4,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @Override
    public Document fetchDocument(String url, ProxyInfo proxy) throws IOException {
        return tryFetch(url, proxy);
    }

    private Document tryFetch(String url, ProxyInfo proxy) throws IOException {
        try {
            Connection connection = createConnection(url, proxy);
            return connection.get();
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Non-retryable HTTP 404 for URL: {}", url);
                throw new NonRetryableException("404 Not Found: " + url, e);
            }
            if (e.getStatusCode() == 429 || e.getStatusCode() == 502) {
                log.warn("Retryable HTTP error {} for URL: {}", e.getStatusCode(), url);
                throw e;
            }
            throw new NonRetryableException("HTTP error: " + e.getStatusCode(), e);
        } catch (SocketException e) {
            log.warn("SocketException for URL {}: {}", url, e.getMessage());
            throw e;
        } catch (IOException e) {
            log.warn("IOException for URL {}: {}", url, e.getMessage());
            throw e;
        }
    }

    private Connection createConnection(String url, ProxyInfo proxy) {
        String userAgent = UserAgentProvider.getRandomUserAgent();
        Connection connection = Jsoup.connect(url)
                .userAgent(userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .timeout(30000);
        if (proxy != null) {
            connection.proxy(proxy.host(), proxy.port());
        }
        return connection;
    }

    @Recover
    public Document recover(IOException e, String url, ProxyInfo proxy) {
        log.error("Failed to fetch document from URL {} after retries: {}", url, e.getMessage());
        return null;
    }
}