package com.webscraper.services;

import com.webscraper.entities.CompressionResult;
import com.webscraper.entities.ImageEntity;
import com.webscraper.repositories.ImageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ImageProcessingService {

    private static final Path OUTPUT_DIRECTORY = Paths.get("C:\\Users\\igors\\IdeaProjects\\WebScraper\\src\\main\\resources\\compressed-images");
    private final RestTemplate restTemplate;
    private final JpegCompressor jpegCompressor;
    private final ImageRepository imageRepository;
    private final ConcurrentMap<String, Boolean> processedImagesCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Path> domainDirectories = new ConcurrentHashMap<>();

    public ImageProcessingService(RestTemplateBuilder restTemplateBuilder,
                                  ImageRepository imageRepository,
                                  JpegCompressor jpegCompressor) {
        this.imageRepository = imageRepository;
        this.jpegCompressor = jpegCompressor;
        // Налаштовуємо таймаути для RestTemplate: 5 секунд для з'єднання та читання
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        createOutputDirectory();
    }

    private void createOutputDirectory() {
        try {
            Files.createDirectories(OUTPUT_DIRECTORY);
        } catch (Exception e) {
            log.error("Не вдалося створити директорію для збереження зображень: {}", OUTPUT_DIRECTORY, e);
        }
    }

    private Path getDomainOutputDirectory(String domain) {
        return domainDirectories.computeIfAbsent(domain, d -> {
            Path domainDir = OUTPUT_DIRECTORY.resolve(d);
            try {
                Files.createDirectories(domainDir);
            } catch (Exception e) {
                log.error("Не вдалося створити директорію для домена {}: {}", d, domainDir, e);
            }
            return domainDir;
        });
    }

    /**
     * Метод для підготовки URL – декодує його та прибирає параметри запиту.
     */
    public String prepareImageUrl(String imageUrl) {
        try {
            String decodedUrl = URLDecoder.decode(imageUrl, StandardCharsets.UTF_8);
            int paramIndex = decodedUrl.indexOf("?");
            if (paramIndex > 0) {
                decodedUrl = decodedUrl.substring(0, paramIndex);
            }
            return decodedUrl;
        } catch (Exception ex) {
            log.warn("Не вдалося декодувати URL: {}. Використовуємо оригінал.", imageUrl, ex);
            return imageUrl;
        }
    }

    /**
     * Головний метод обробки зображення, який визначає тип URL і отримує відповідні байти.
     */
    public void processImage(String imagePath, String domain) {
        if (isAlreadyProcessed(imagePath)) {
            log.info("Зображення {} вже оброблено (знайдено в локальному кеші).", imagePath);
            return;
        }
        try {
            byte[] imageBytes = getImageBytes(imagePath);
            if (imageBytes == null) {
                log.warn("Не вдалося отримати дані зображення для URL: {}", imagePath);
                return;
            }
            if (imageBytes.length < 200 * 1024) {
                log.info("Зображення {} менше 200 КБ, пропускаємо обробку.", imagePath);
                return;
            }
            processImageBytes(imageBytes, imagePath, domain);
        } catch (Exception ex) {
            log.error("Помилка обробки зображення {}: ", imagePath, ex);
        }
    }

    /**
     * Визначає тип URL та повертає байти зображення.
     */
    private byte[] getImageBytes(String imageUrl) {
        if (isDataUri(imageUrl)) {
            return processDataUriImage(imageUrl);
        } else if (isTemplateUrl(imageUrl)) {
            return processTemplateImage(imageUrl);
        } else {
            return processRegularImage(imageUrl);
        }
    }

    private byte[] processTemplateImage(String imageUrl) {
        Map<String, String> uriVariables = getUriVariablesForImage(imageUrl);
        if (uriVariables == null || uriVariables.isEmpty()) {
            log.warn("URL {} містить шаблонні змінні, але значення для них не передано. Пропускаємо обробку.", imageUrl);
            return null;
        }
        try {
            return restTemplate.getForObject(imageUrl, byte[].class, uriVariables);
        } catch (Exception ex) {
            log.error("Помилка обробки шаблонного URL {}: ", imageUrl, ex);
            return null;
        }
    }

    private byte[] processDataUriImage(String imageUrl) {
        try {
            return decodeDataUri(imageUrl);
        } catch (Exception ex) {
            log.error("Помилка обробки data URI {}: ", imageUrl, ex);
            return null;
        }
    }

    private byte[] processRegularImage(String imageUrl) {
        String preparedUrl = prepareImageUrl(imageUrl);
        log.debug("Підготовлений URL для зображення: {}", preparedUrl);
        try {
            return restTemplate.getForObject(preparedUrl, byte[].class);
        } catch (Exception ex) {
            log.error("Помилка обробки URL {}: ", imageUrl, ex);
            return null;
        }
    }

    private void processImageBytes(byte[] imageBytes, String imagePath, String domain) {
        if (imageBytes == null) {
            log.warn("Не вдалося отримати дані зображення за URL: {}", imagePath);
            return;
        }
        try {
            Path domainDir = getDomainOutputDirectory(domain);
            CompressionResult result = jpegCompressor.compressAndSave(imageBytes, domainDir);

            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setOriginalUrl(imagePath);
            imageEntity.setPath(result.fileLink());
            imageEntity.setOriginalSize(imageBytes.length);
            imageEntity.setSizeAfterCompression(result.compressedSize());
            // imageRepository.save(imageEntity);
            processedImagesCache.put(imagePath, Boolean.TRUE);
        } catch (Exception e) {
            log.error("Помилка під час обробки зображення за URL {}: ", imagePath, e);
        }
    }

    private boolean isAlreadyProcessed(String imagePath) {
        return processedImagesCache.containsKey(imagePath);
    }

    private boolean isTemplateUrl(String url) {
        return url.contains("{") && url.contains("}");
    }

    private boolean isDataUri(String url) {
        return url.startsWith("data:");
    }

    /**
     * Для шаблонних URL повертає мапу параметрів.
     * Покращено для обробки будь-якої кількості шаблонних змінних, які знаходяться у фігурних дужках.
     */
    private Map<String, String> getUriVariablesForImage(String imagePath) {
        Map<String, String> uriVariables = new HashMap<>();
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(imagePath);
        while (matcher.find()) {
            String key = matcher.group(1);
            // Тут можна додати більш гнучку логіку, наприклад,
            // отримувати значення з конфігурації або зовнішнього джерела.
            uriVariables.put(key, "defaultValue");
        }
        return uriVariables;
    }

    /**
     * Декодує data URI з перевіркою на маркер base64.
     * Якщо дані не кодуються в base64, вони сприймаються як URL-кодований текст.
     */
    private byte[] decodeDataUri(String dataUri) {
        int commaIndex = dataUri.indexOf(',');
        if (commaIndex == -1) {
            throw new IllegalArgumentException("Невірний формат data URI: " + dataUri);
        }
        String meta = dataUri.substring(0, commaIndex);
        String data = dataUri.substring(commaIndex + 1);
        if (meta.contains(";base64")) {
            try {
                return Base64.getDecoder().decode(data);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Невірний формат Base64 даних у data URI: " + dataUri, e);
            }
        } else {
            try {
                // Якщо дані не кодуються в Base64, виконуємо URL-декодування
                String decoded = URLDecoder.decode(data, StandardCharsets.UTF_8);
                return decoded.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IllegalArgumentException("Невдале декодування даних з data URI: " + dataUri, e);
            }
        }
    }
}
