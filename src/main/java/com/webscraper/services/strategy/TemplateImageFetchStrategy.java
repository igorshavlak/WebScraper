package com.webscraper.services.strategy;

import com.webscraper.services.ImageFetchStrategy;
import com.webscraper.services.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TemplateImageFetchStrategy implements ImageFetchStrategy {

    private final RestTemplate restTemplate;


    public TemplateImageFetchStrategy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean supports(String imageUrl) {
        return imageUrl.contains("{") && imageUrl.contains("}");
    }

    @Override
    public byte[] fetchImage(String imageUrl, ImageProcessingService context) {
        Map<String, String> uriVariables = extractUriVariables(imageUrl);
        if (uriVariables.isEmpty()) {
            log.warn("URL {} містить шаблонні змінні, але значення для них не передано.", imageUrl);
            return null;
        }
        try {
            return restTemplate.getForObject(imageUrl, byte[].class, uriVariables);
        } catch (Exception ex) {
            log.error("Помилка обробки шаблонного URL {}: {}", imageUrl, ex.getMessage());
            return null;
        }
    }

    private Map<String, String> extractUriVariables(String imageUrl) {
        Map<String, String> variables = new HashMap<>();
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(imageUrl);
        while (matcher.find()) {
            String key = matcher.group(1);
            variables.put(key, "defaultValue");
        }
        return variables;
    }
}