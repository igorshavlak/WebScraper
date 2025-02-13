package com.webscraper.services.impl;

import com.webscraper.entities.ProxyInfo;
import com.webscraper.services.ProxySelectorService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RoundRobinProxySelectorService implements ProxySelectorService {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public ProxyInfo selectProxy(List<ProxyInfo> proxies) {
        if (proxies == null || proxies.isEmpty()) {
            return null;
        }
        int pos = index.getAndIncrement();
        return proxies.get(pos % proxies.size());
    }
}