package com.webscraper.services;


import com.webscraper.entities.ProxyInfo;

import java.util.List;

public interface ProxySelectorService {

    ProxyInfo selectProxy(List<ProxyInfo> proxies);
}