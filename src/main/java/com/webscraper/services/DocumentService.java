package com.webscraper.services;

import com.webscraper.entities.ProxyInfo;
import org.jsoup.nodes.Document;

import java.io.IOException;

public interface DocumentService {
     Document fetchDocument(String url, ProxyInfo proxy) throws IOException;
}
