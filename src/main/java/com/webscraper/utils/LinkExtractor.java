package com.webscraper.utils;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for extracting links and image URLs from JSoup documents.
 */
@Slf4j
public class LinkExtractor {

    /**
     * Extracts all hyperlink URLs from the given document, excluding those ending with common image file extensions.
     *
     * @param document the JSoup Document to extract links from
     * @return a Set of absolute URLs as Strings
     */
    public static Set<String> extractLinks(Document document) {
        Set<String> links = new HashSet<>();
        Elements linkElements = document.select("a[href]");
        for (Element element : linkElements) {
            String href = element.attr("abs:href");
            if (!href.matches("(?i).*\\.(png|jpg|jpeg|gif|bmp)(\\?.*)?$")) {
                links.add(href);
            }
        }
        return links;
    }

    /**
     * Extracts all image URLs from the given document based on <code>img</code> tags.
     *
     * @param document the JSoup Document to extract image URLs from
     * @return a Set of absolute image URLs as Strings
     */
    public static Set<String> extractImages(Document document) {
        Set<String> imagesLinks = new HashSet<>();
        Elements images = document.select("img[src]");
        for (Element element : images) {
            imagesLinks.add(element.attr("abs:src"));
        }
        return imagesLinks;
    }

    /**
     * Extracts image URLs from inline CSS style attributes and embedded &lt;style&gt; tags.
     *
     * @param document the JSoup Document to extract CSS-based image URLs from
     * @return a Set of absolute image URLs as Strings
     */
    public static Set<String> extractCssImages(Document document) {
        Set<String> cssImages = new HashSet<>();
        Pattern pattern = Pattern.compile("url\\(['\"]?(.*?)['\"]?\\)");

        // Extract from elements with inline styles
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

        // Extract from <style> tags
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

    /**
     * Extracts image URLs that are contained within anchor (<code>a</code>) tags.
     *
     * @param document the JSoup Document to extract image links from
     * @return a Set of absolute image URLs as Strings
     */
    public static Set<String> extractAnchorImageLinks(Document document) {
        Set<String> imageLinks = new HashSet<>();
        Elements anchorElements = document.select("a[href]");
        for (Element element : anchorElements) {
            String href = element.attr("abs:href");
            if (href.matches("(?i).*\\.(png|jpg|jpeg|gif|bmp)(\\?.*)?$")) {
                imageLinks.add(href);
            }
        }
        return imageLinks;
    }

    /**
     * Resolves a potentially relative image URL against the provided base URI.
     *
     * @param baseUri  the base URI of the document
     * @param imageUrl the image URL to resolve
     * @return the resolved absolute URL as a String, or an empty string if resolution fails
     */
    private static String resolveUrl(String baseUri, String imageUrl) {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        try {
            URI base = new URI(baseUri);
            URI resolved = base.resolve(imageUrl);
            return resolved.toString();
        } catch (URISyntaxException e) {
            log.error("Failed to resolve URL: {} with base URI: {}. Error: {}", imageUrl, baseUri, e.getMessage());
            return "";
        }
    }
}
