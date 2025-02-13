package com.webscraper.utils;

import com.webscraper.entities.ProxyInfo;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

public class ProxyCheckerService {

    public static List<ProxyInfo> filterWorkingProxies(List<ProxyInfo> proxies) {
        if (proxies == null || proxies.isEmpty()) {
            return proxies;
        }
        proxies.removeIf(proxy -> !isProxyWorking(proxy));
        return proxies;
    }

    private static boolean isProxyWorking(ProxyInfo proxy) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new SecureRandom());

            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(5))
                    .proxy(ProxySelector.of(new InetSocketAddress("35.72.118.126", 80)))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://www.google.com"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
