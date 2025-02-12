package com.webscraper.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;



@Slf4j
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
    public static Set<String> extractCssImages(Document document) {
        Set<String> cssImages = new HashSet<>();
        Pattern pattern = Pattern.compile("url\\(['\"]?(.*?)['\"]?\\)");

        Elements elementsWithStyle = document.select("[style]");
        for (Element element : elementsWithStyle) {
            String style = element.attr("style");
            Matcher matcher = pattern.matcher(style);
            while (matcher.find()) {
                String imageUrl = matcher.group(1);
                imageUrl = resolveUrl(document.baseUri(), imageUrl);
                if (!imageUrl.isEmpty()) {
                    cssImages.add(imageUrl);
                }
            }
        }

        Elements styleTags = document.select("style");
        for (Element styleTag : styleTags) {
            String css = styleTag.data();
            Matcher matcher = pattern.matcher(css);
            while (matcher.find()) {
                String imageUrl = matcher.group(1);
                imageUrl = resolveUrl(document.baseUri(), imageUrl);
                if (!imageUrl.isEmpty()) {
                    cssImages.add(imageUrl);
                }
            }
        }
        return cssImages;
    }
    private static String resolveUrl(String baseUri, String imageUrl) {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        try {
            URI base = new URI(baseUri);
            URI resolved = base.resolve(imageUrl);
            return resolved.toString();
        } catch (URISyntaxException e) {
            log.error("Не вдалося вирішити URL: {} з базовим URI: {}. Помилка: {}", imageUrl, baseUri, e.getMessage());
            return "";
        }
    }

}
