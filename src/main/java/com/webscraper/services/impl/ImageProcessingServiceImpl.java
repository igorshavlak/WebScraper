package com.webscraper.services.impl;

import com.webscraper.entities.CompressionResult;
import com.webscraper.entities.ImageEntity;
import com.webscraper.repositories.ImageRepository;
import com.webscraper.services.ImageProcessingService;
import com.webscraper.services.JpegCompressor;
import com.webscraper.services.strategy.ImageFetchStrategy;
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

/**
 * Implementation of {@link ImageProcessingService} for processing images:
 * fetching, compressing, and preparing URLs.
 */
@Slf4j
@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private final JpegCompressor jpegCompressor;
    private final ImageRepository imageRepository;
    private final ConcurrentMap<String, Boolean> processedImagesCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Path> domainDirectories = new ConcurrentHashMap<>();
    private final List<ImageFetchStrategy> imageFetchStrategies;
    private final Path outputDirectory;

    /**
     * Constructs a new ImageProcessingServiceImpl.
     *
     * @param restTemplateBuilder a RestTemplateBuilder for HTTP requests
     * @param imageRepository     the repository for storing image data
     * @param jpegCompressor      the JPEG compressor service
     * @param imageFetchStrategies the list of strategies to fetch images
     * @param env                 the environment for configuration properties
     */
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

    /**
     * Creates the output directory if it does not exist.
     */
    private void createOutputDirectory() {
        try {
            Files.createDirectories(outputDirectory);
        } catch (Exception e) {
            log.error("Failed to create directory: {}", outputDirectory, e);
        }
    }

    /**
     * Retrieves (or creates) the directory for a specific domain.
     *
     * @param domain the domain name
     * @return the {@link Path} to the domain-specific directory
     */
    private Path getDomainOutputDirectory(String domain) {
        return domainDirectories.computeIfAbsent(domain, d -> {
            Path domainDir = outputDirectory.resolve(d);
            try {
                Files.createDirectories(domainDir);
            } catch (Exception e) {
                log.error("Failed to create directory for domain {}: {}", d, domainDir, e);
            }
            return domainDir;
        });
    }

    /**
     * Processes the image at the given path by fetching, compressing, and saving it.
     *
     * @param imagePath the URL or path of the image
     * @param domain    the domain associated with the image
     */
    @Override
    public void processImage(String imagePath, String domain) {
        if (processedImagesCache.containsKey(imagePath)) {
            log.info("Image {} has already been processed.", imagePath);
            return;
        }
        try {
            byte[] imageBytes = getImageBytes(imagePath);
            if (imageBytes == null) {
                log.warn("Failed to obtain image data for URL: {}", imagePath);
                return;
            }
            if (imageBytes.length < 200 * 1024) {
                log.info("Image {} is less than 200 KB; skipping processing.", imagePath);
                return;
            }
            processImageBytes(imageBytes, imagePath, domain);
        } catch (Exception ex) {
            log.error("Error processing image {}: ", imagePath, ex);
        }
    }

    /**
     * Attempts to fetch image bytes using the registered image fetch strategies.
     *
     * @param imageUrl the URL of the image
     * @return a byte array containing the image data, or null if no strategy supports the URL
     */
    private byte[] getImageBytes(String imageUrl) {
        for (ImageFetchStrategy strategy : imageFetchStrategies) {
            if (strategy.supports(imageUrl)) {
                return strategy.fetchImage(imageUrl, this);
            }
        }
        log.warn("No fetch strategy found for image URL: {}", imageUrl);
        return null;
    }

    /**
     * Prepares the image URL by decoding it and removing query parameters.
     *
     * @param imageUrl the original image URL
     * @return the prepared image URL
     */
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
            log.warn("Failed to decode URL: {}. Using original.", imageUrl, ex);
            return imageUrl;
        }
    }

    /**
     * Compresses and saves the image bytes, then updates the repository.
     *
     * @param imageBytes the original image data
     * @param imagePath  the original image URL or path
     * @param domain     the domain associated with the image
     */
    private void processImageBytes(byte[] imageBytes, String imagePath, String domain) {
        try {
            Path domainDir = getDomainOutputDirectory(domain);
            CompressionResult result = jpegCompressor.compressAndSave(imageBytes, domainDir);

            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setOriginalUrl(imagePath);
            imageEntity.setPath(result.fileLink());
            imageEntity.setOriginalSize(imageBytes.length);
            imageEntity.setSizeAfterCompression(result.compressedSize());
            imageRepository.save(imageEntity);
            processedImagesCache.put(imagePath, Boolean.TRUE);
        } catch (Exception e) {
            log.error("Error processing image {}: ", imagePath, e);
        }
    }
}
