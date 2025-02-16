package com.webscraper.services.strategy;

import com.webscraper.services.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link ImageFetchStrategy} for handling image URLs that contain template variables.
 */
@Slf4j
@Component
public class TemplateImageFetchStrategy implements ImageFetchStrategy {

    private final RestTemplate restTemplate;

    /**
     * Constructs a TemplateImageFetchStrategy with the provided RestTemplate.
     *
     * @param restTemplate the RestTemplate to use for HTTP requests
     */
    public TemplateImageFetchStrategy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Checks if the image URL contains template variables (denoted by curly braces).
     *
     * @param imageUrl the image URL to check
     * @return true if the URL contains '{' and '}', false otherwise
     */
    @Override
    public boolean supports(String imageUrl) {
        return imageUrl.contains("{") && imageUrl.contains("}");
    }

    /**
     * Fetches the image data from a URL that contains template variables.
     *
     * @param imageUrl the image URL with template variables
     * @param context  the image processing context
     * @return a byte array containing the image data, or null if an error occurs
     */
    @Override
    public byte[] fetchImage(String imageUrl, ImageProcessingService context) {
        Map<String, String> uriVariables = extractUriVariables(imageUrl);
        if (uriVariables.isEmpty()) {
            log.warn("URL {} contains template variables but no values were provided.", imageUrl);
            return null;
        }
        try {
            return restTemplate.getForObject(imageUrl, byte[].class, uriVariables);
        } catch (Exception ex) {
            log.error("Error processing template URL {}: {}", imageUrl, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Extracts template variables from the image URL.
     *
     * @param imageUrl the image URL containing template variables
     * @return a map of variable names to default values
     */
    private Map<String, String> extractUriVariables(String imageUrl) {
        Map<String, String> variables = new HashMap<>();
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(imageUrl);
        while (matcher.find()) {
            String key = matcher.group(1);
            // Встановлюємо дефолтне значення для змінної (можна змінити логіку за потребою)
            variables.put(key, "defaultValue");
        }
        return variables;
    }
}
