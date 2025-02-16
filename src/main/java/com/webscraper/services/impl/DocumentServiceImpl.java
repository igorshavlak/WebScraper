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

/**
 * Implementation of {@link DocumentService} for fetching HTML documents using JSoup.
 */
@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {

    /**
     * Fetches the document from the given URL using the provided proxy.
     * <p>
     * This method is retryable in case of IO, Socket, or HTTP status exceptions.
     *
     * @param url   the URL to fetch
     * @param proxy the proxy information; may be null
     * @return the fetched JSoup Document
     * @throws IOException if fetching fails after retries
     */
    @Retryable(
            value = { IOException.class, SocketException.class, HttpStatusException.class },
            maxAttempts = 4,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @Override
    public Document fetchDocument(String url, ProxyInfo proxy) throws IOException {
        return tryFetch(url, proxy);
    }

    /**
     * Attempts to fetch the document and handles specific HTTP status exceptions.
     *
     * @param url   the URL to fetch
     * @param proxy the proxy information; may be null
     * @return the fetched JSoup Document
     * @throws IOException if an error occurs during fetching
     */
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

    /**
     * Creates a JSoup connection for the given URL with the appropriate headers and proxy settings.
     *
     * @param url   the URL to connect to
     * @param proxy the proxy information; may be null
     * @return the JSoup {@link Connection} instance
     */
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

    /**
     * Recovery method for fetchDocument after all retries have been exhausted.
     *
     * @param e     the exception encountered during fetching
     * @param url   the URL that was being fetched
     * @param proxy the proxy information used
     * @return null, indicating that the document could not be fetched
     */
    @Recover
    public Document recover(IOException e, String url, ProxyInfo proxy) {
        log.error("Failed to fetch document from URL {} after retries: {}", url, e.getMessage());
        return null;
    }
}
