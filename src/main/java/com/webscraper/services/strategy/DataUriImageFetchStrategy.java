package com.webscraper.services.strategy;

import com.webscraper.services.ImageFetchStrategy;
import com.webscraper.services.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class DataUriImageFetchStrategy implements ImageFetchStrategy {

    @Override
    public boolean supports(String imageUrl) {
        return imageUrl.startsWith("data:");
    }

    @Override
    public byte[] fetchImage(String imageUrl, ImageProcessingService context) {
        int commaIndex = imageUrl.indexOf(',');
        if (commaIndex == -1) {
            throw new IllegalArgumentException("Невірний формат data URI: " + imageUrl);
        }
        String meta = imageUrl.substring(0, commaIndex);
        String data = imageUrl.substring(commaIndex + 1);
        if (meta.contains(";base64")) {
            try {
                return Base64.getDecoder().decode(data);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Невірний формат Base64 у data URI: " + imageUrl, e);
            }
        } else {
            try {
                String decoded = URLDecoder.decode(data, StandardCharsets.UTF_8);
                return decoded.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IllegalArgumentException("Невдале декодування data URI: " + imageUrl, e);
            }
        }
    }
}