package com.webscraper.services.strategy;

import com.webscraper.services.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Implementation of {@link ImageFetchStrategy} to handle images encoded as Data URIs.
 */
@Slf4j
@Component
public class DataUriImageFetchStrategy implements ImageFetchStrategy {

    /**
     * Checks if the image URL is a Data URI.
     *
     * @param imageUrl the image URL to check
     * @return true if the URL starts with "data:", false otherwise
     */
    @Override
    public boolean supports(String imageUrl) {
        return imageUrl.startsWith("data:");
    }

    /**
     * Fetches image data from a Data URI.
     *
     * @param imageUrl the Data URI
     * @param context  the image processing context
     * @return a byte array containing the decoded image data
     * @throws IllegalArgumentException if the Data URI format is invalid
     */
    @Override
    public byte[] fetchImage(String imageUrl, ImageProcessingService context) {
        int commaIndex = imageUrl.indexOf(',');
        if (commaIndex == -1) {
            throw new IllegalArgumentException("Invalid data URI format: " + imageUrl);
        }
        String meta = imageUrl.substring(0, commaIndex);
        String data = imageUrl.substring(commaIndex + 1);
        if (meta.contains(";base64")) {
            try {
                return Base64.getDecoder().decode(data);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid Base64 format in data URI: " + imageUrl, e);
            }
        } else {
            try {
                String decoded = URLDecoder.decode(data, StandardCharsets.UTF_8);
                return decoded.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to decode data URI: " + imageUrl, e);
            }
        }
    }
}
