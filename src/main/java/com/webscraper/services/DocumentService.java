package com.webscraper.services;

import com.webscraper.entities.ProxyInfo;
import org.jsoup.nodes.Document;
import java.io.IOException;

/**
 * Service interface for fetching HTML documents.
 */
public interface DocumentService {

     /**
      * Fetches the HTML document from the specified URL using the provided proxy.
      *
      * @param url   the URL of the document to fetch
      * @param proxy the proxy information to use for the connection; may be null
      * @return the fetched JSoup Document
      * @throws IOException if an error occurs during fetching
      */
     Document fetchDocument(String url, ProxyInfo proxy) throws IOException;
}
