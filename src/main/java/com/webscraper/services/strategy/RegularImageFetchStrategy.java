package com.webscraper.services.strategy;
import com.webscraper.services.ImageFetchStrategy;
import com.webscraper.services.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class RegularImageFetchStrategy implements ImageFetchStrategy {

    private final RestTemplate restTemplate;

    public RegularImageFetchStrategy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean supports(String imageUrl) {
        return !imageUrl.startsWith("data:") && !(imageUrl.contains("{") && imageUrl.contains("}"));
    }

    @Override
    public byte[] fetchImage(String imageUrl, ImageProcessingService context) {
        String preparedUrl = context.prepareImageUrl(imageUrl);
        try {
            return restTemplate.getForObject(preparedUrl, byte[].class);
        } catch (Exception ex) {
            log.error("Помилка обробки URL {}: {}", imageUrl, ex.getMessage());
            return null;
        }
    }
}