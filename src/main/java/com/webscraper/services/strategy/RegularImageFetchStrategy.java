package com.webscraper.services.strategy;

import com.webscraper.services.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of {@link ImageFetchStrategy} for fetching regular images via HTTP.
 */
@Slf4j
@Component
public class RegularImageFetchStrategy implements ImageFetchStrategy {

    private final RestTemplate restTemplate;

    /**
     * Constructs a RegularImageFetchStrategy with the provided RestTemplate.
     *
     * @param restTemplate the RestTemplate to use for HTTP requests
     */
    public RegularImageFetchStrategy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Checks if this strategy supports the given image URL.
     *
     * @param imageUrl the image URL to check
     * @return true if the URL is not a Data URI and does not contain template variables; false otherwise
     */
    @Override
    public boolean supports(String imageUrl) {
        return !imageUrl.startsWith("data:") && !(imageUrl.contains("{") && imageUrl.contains("}"));
    }

    /**
     * Fetches the image data from the specified URL using HTTP GET.
     *
     * @param imageUrl the image URL
     * @param context  the image processing context
     * @return a byte array containing the image data, or null if an error occurs
     */
    @Override
    public byte[] fetchImage(String imageUrl, ImageProcessingService context) {
        String preparedUrl = context.prepareImageUrl(imageUrl);
        try {
            return restTemplate.getForObject(preparedUrl, byte[].class);
        } catch (Exception ex) {
            log.error("Error processing URL {}: {}", imageUrl, ex.getMessage(), ex);
            return null;
        }
    }
}
