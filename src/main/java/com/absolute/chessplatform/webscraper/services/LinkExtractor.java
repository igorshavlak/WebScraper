package com.absolute.chessplatform.webscraper.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;


@Slf4j
@Component
public class LinkExtractor {

    public static Set<String> extractLinks(Document document) {
        Set<String> links = new HashSet<>();
        Elements linkElements = document.select("a[href]");
        for (Element element : linkElements) {
            links.add(element.attr("abs:href"));
        }
        return links;
    }
    public static Set<String> extractImages(Document document)  {
        Set<String> imagesLinks = new HashSet<>();
        Elements images = document.select("img[src]");
        for (Element element : images) {
           imagesLinks.add(element.attr("abs:src"));
        }
        return imagesLinks;
    }

    public static boolean isLinkRefersToDomain(String url, String domain) throws URISyntaxException {
        String host = new URI(url).getHost();
        if (host == null) {
            return false;
        }
        return host.equalsIgnoreCase(domain);
    }
}
