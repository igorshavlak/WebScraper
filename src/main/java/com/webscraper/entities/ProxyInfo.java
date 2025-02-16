package com.webscraper.entities;

/**
 * Record representing proxy server information.
 *
 * @param host the hostname or IP address of the proxy
 * @param port the port number of the proxy
 */
public record ProxyInfo(String host, int port) {
}
