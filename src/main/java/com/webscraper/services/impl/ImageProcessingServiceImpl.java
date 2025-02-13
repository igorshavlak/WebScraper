package com.webscraper.services.impl;

import com.webscraper.entities.CompressionResult;
import com.webscraper.entities.ImageEntity;
import com.webscraper.repositories.ImageRepository;
import com.webscraper.services.ImageProcessingService;
import com.webscraper.services.JpegCompressor;
import com.webscraper.services.ImageFetchStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private final JpegCompressor jpegCompressor;
    private final ImageRepository imageRepository;
    private final ConcurrentMap<String, Boolean> processedImagesCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Path> domainDirectories = new ConcurrentHashMap<>();
    private final List<ImageFetchStrategy> imageFetchStrategies;
    private final Path outputDirectory;

    @Autowired
    public ImageProcessingServiceImpl(RestTemplateBuilder restTemplateBuilder,
                                      ImageRepository imageRepository,
                                      JpegCompressor jpegCompressor,
                                      List<ImageFetchStrategy> imageFetchStrategies,
                                      Environment env) {
        this.imageRepository = imageRepository;
        this.jpegCompressor = jpegCompressor;
        this.imageFetchStrategies = imageFetchStrategies;
        String outputDirStr = env.getProperty("images.output.directory", "compressed-images");
        this.outputDirectory = Paths.get(outputDirStr);
        createOutputDirectory();
    }

    private void createOutputDirectory() {
        try {
            Files.createDirectories(outputDirectory);
        } catch (Exception e) {
            log.error("Не вдалося створити директорію: {}", outputDirectory, e);
        }
    }

    private Path getDomainOutputDirectory(String domain) {
        return domainDirectories.computeIfAbsent(domain, d -> {
            Path domainDir = outputDirectory.resolve(d);
            try {
                Files.createDirectories(domainDir);
            } catch (Exception e) {
                log.error("Не вдалося створити директорію для домена {}: {}", d, domainDir, e);
            }
            return domainDir;
        });
    }

    @Override
    public void processImage(String imagePath, String domain) {
        if (processedImagesCache.containsKey(imagePath)) {
            log.info("Зображення {} вже оброблено.", imagePath);
            return;
        }
        try {
            byte[] imageBytes = getImageBytes(imagePath);
            if (imageBytes == null) {
                log.warn("Не вдалося отримати дані зображення для URL: {}", imagePath);
                return;
            }
            if (imageBytes.length < 200 * 1024) {
                log.info("Зображення {} менше 200 КБ, обробку пропускаємо.", imagePath);
                return;
            }
            processImageBytes(imageBytes, imagePath, domain);
        } catch (Exception ex) {
            log.error("Помилка обробки зображення {}: ", imagePath, ex);
        }
    }

    private byte[] getImageBytes(String imageUrl) {
        for (ImageFetchStrategy strategy : imageFetchStrategies) {
            if (strategy.supports(imageUrl)) {
                return strategy.fetchImage(imageUrl, this);
            }
        }
        log.warn("Не знайдено стратегію для URL зображення: {}", imageUrl);
        return null;
    }

    @Override
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

    private void processImageBytes(byte[] imageBytes, String imagePath, String domain) {
        try {
            Path domainDir = getDomainOutputDirectory(domain);
            CompressionResult result = jpegCompressor.compressAndSave(imageBytes, domainDir);

            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setOriginalUrl(imagePath);
            imageEntity.setPath(result.fileLink());
            imageEntity.setOriginalSize(imageBytes.length);
            imageEntity.setSizeAfterCompression(result.compressedSize());
            // imageRepository.save(imageEntity); // збереження у базі даних (якщо потрібно)
            processedImagesCache.put(imagePath, Boolean.TRUE);
        } catch (Exception e) {
            log.error("Помилка обробки зображення {}: ", imagePath, e);
        }
    }
}
