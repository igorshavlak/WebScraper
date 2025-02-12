package com.webscraper.services;

import com.webscraper.entities.ProxyInfo;
import com.webscraper.exceptions.NonRetryableException;
import com.webscraper.providers.UserAgentProvider;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.SocketException;

@Slf4j
@Service
public class DocumentService {

    private static final int MAX_RETRY_ATTEMPTS = 4;
    private static final long INITIAL_RETRY_DELAY_MS = 2000;

    public Document fetchDocument(String url, ProxyInfo proxy) {
        int attempts = 0;
        long delay = INITIAL_RETRY_DELAY_MS;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                Document doc = tryFetch(url, proxy);
                if (doc != null) {
                    return doc;
                }
            } catch (NonRetryableException e) {
                log.warn("Помилка, що не підлягає повторній спробі для URL {}: {}", url, e.getMessage());
                return null;
            }
            attempts++;
            log.warn("Спроба {} для URL {} не вдалася. Повторна спроба через {} мс...", attempts, url, delay);
            sleep(delay);
            delay *= 2;
        }
        log.error("Не вдалося отримати документ за URL {} після {} спроб", url, MAX_RETRY_ATTEMPTS);
        return null;
    }

    private Document tryFetch(String url, ProxyInfo proxy) throws NonRetryableException {
        try {
            Connection connection = createConnection(url, proxy);
            return connection.get();
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 429 || e.getStatusCode() == 502) {
                log.warn("Отримано HTTP {} для URL {}. Буде зроблено повторну спробу.", e.getStatusCode(), url);
                return null;
            } else {
                log.warn("Отримано HTTP {} для URL {}. Повторна спроба не виконується.", e.getStatusCode(), url);
                throw new NonRetryableException(e.getMessage(), e);
            }
        } catch (SocketException e) {
            log.warn("SocketException для URL {}: {}", url, e.getMessage());
            throw new NonRetryableException(e.getMessage(), e);
        } catch (IOException e) {
            log.warn("IOException для URL {}: {}", url, e.getMessage());
            throw new NonRetryableException(e.getMessage(), e);
        }
    }

    private Connection createConnection(String url, ProxyInfo proxy) {
        String userAgent = UserAgentProvider.getRandomUserAgent();
        Connection connection = Jsoup.connect(url)
                .userAgent(userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .timeout(30_000);
        if (proxy != null) {
            connection.proxy(proxy.host(), proxy.port());
        }
        return connection;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
