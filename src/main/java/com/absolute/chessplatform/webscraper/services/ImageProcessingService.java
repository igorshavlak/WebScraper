package com.absolute.chessplatform.webscraper.services;

import com.absolute.chessplatform.webscraper.entities.CompressionResult;
import com.absolute.chessplatform.webscraper.entities.ImageEntity;
import com.absolute.chessplatform.webscraper.repositories.ImageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class ImageProcessingService {

    private static final Path OUTPUT_DIRECTORY = Paths.get("C:\\Users\\igors\\IdeaProjects\\WebScraper\\src\\main\\resources\\compressed-images");
    private final RestTemplate restTemplate;
    private final JpegCompressor jpegCompressor;
    private final ImageRepository imageRepository;
    private final ConcurrentMap<String, Boolean> processedImagesCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Path> domainDirectories = new ConcurrentHashMap<>();



    public ImageProcessingService(RestTemplateBuilder restTemplateBuilder, ImageRepository imageRepository, JpegCompressor jpegCompressor) {
        this.imageRepository = imageRepository;
        this.jpegCompressor = jpegCompressor;
        this.restTemplate = restTemplateBuilder.build();
        createOutputDirectory();
    }
    private void createOutputDirectory() {
        try {
            Files.createDirectories(OUTPUT_DIRECTORY);
        } catch (IOException e) {
            log.error("Не вдалося створити директорію для збереження зображень: {}", OUTPUT_DIRECTORY, e);
        }
    }
    private Path getDomainOutputDirectory(String domain) {
        return domainDirectories.computeIfAbsent(domain, d -> {
            Path domainDir = OUTPUT_DIRECTORY.resolve(d);
            try {
                Files.createDirectories(domainDir);
            } catch (IOException e) {
                log.error("Не вдалося створити директорію для домена {}: {}", d, domainDir, e);
            }
            return domainDir;
        });
    }

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

    public void processImage(String imagePath, String domain) {
        if (processedImagesCache.containsKey(imagePath)) {
            log.info("Зображення {} вже оброблено (знайдено в локальному кеші).", imagePath);
            return;
        }
        String preparedUrl = prepareImageUrl(imagePath);
        log.debug("Підготовлений URL для зображення: {}", preparedUrl);
        byte[] imageBytes = restTemplate.getForObject(imagePath, byte[].class);
        if (imageBytes == null) {
            log.warn("Не вдалося отримати дані зображення за URL: {}", imagePath);
            return;
        }
//        if (imageBytes.length < 200 * 1024) {
//            log.info("Розмір зображення ({}) менший за 200 КБ, обробка пропущена {}.", imageBytes.length / 1024, imagePath);
//            return;
//        }
        try {
            Path domainDir = getDomainOutputDirectory(domain);
            CompressionResult result = jpegCompressor.compressAndSave(imageBytes, domainDir);

            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setOriginalUrl(imagePath);
            imageEntity.setPath(result.fileLink());
            imageEntity.setOriginalSize(imageBytes.length);
            imageEntity.setSizeAfterCompression(result.compressedSize());
            //imageRepository.save(imageEntity);
            processedImagesCache.put(imagePath, Boolean.TRUE);
        } catch (IOException e) {
            log.error("Помилка під час обробки зображення за URL: {}", imagePath);
        }
    }


}
