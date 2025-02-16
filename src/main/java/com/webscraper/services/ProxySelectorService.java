package com.webscraper.services;

import com.webscraper.entities.ProxyInfo;
import java.util.List;

/**
 * Service interface for selecting a proxy from a given list.
 */
public interface ProxySelectorService {

    /**
     * Selects an appropriate {@link ProxyInfo} from the provided list.
     *
     * @param proxies the list of proxies to choose from
     * @return a selected {@link ProxyInfo} or null if the list is empty or null
     */
    ProxyInfo selectProxy(List<ProxyInfo> proxies);
}
