package com.webscraper.services.impl;

import com.webscraper.entities.ProxyInfo;
import com.webscraper.services.ProxySelectorService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link ProxySelectorService} that selects proxies using a round-robin algorithm.
 */
@Service
public class RoundRobinProxySelectorService implements ProxySelectorService {

    private final AtomicInteger index = new AtomicInteger(0);

    /**
     * Selects a proxy from the list using a round-robin approach.
     *
     * @param proxies the list of proxies to choose from
     * @return a {@link ProxyInfo} from the list or null if the list is empty
     */
    @Override
    public ProxyInfo selectProxy(List<ProxyInfo> proxies) {
        if (proxies == null || proxies.isEmpty()) {
            return null;
        }
        int pos = index.getAndIncrement();
        return proxies.get(pos % proxies.size());
    }
}
