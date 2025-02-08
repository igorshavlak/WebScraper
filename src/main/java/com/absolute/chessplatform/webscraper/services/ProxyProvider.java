package com.absolute.chessplatform.webscraper.services;

import com.absolute.chessplatform.webscraper.entities.ProxyInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@Service
@RequiredArgsConstructor
public class ProxyProvider {
    private final List<ProxyInfo> proxies = List.of(
            new ProxyInfo("3.91.233.113",8118),
            new ProxyInfo("18.223.25.15",80)
            );

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public ProxyInfo getNextProxy() {
        if (proxies.isEmpty()) {
            return null;
        }
        int index = currentIndex.getAndUpdate(i -> (i + 1) % proxies.size());
        return proxies.get(index);
    }
}