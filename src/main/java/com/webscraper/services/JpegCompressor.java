package com.webscraper.services;

import com.webscraper.entities.CompressionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;

/**
 * Service for compressing JPEG images.
 */
@Service
@Slf4j
public class JpegCompressor {

    /**
     * Initial quality setting (configurable via application.properties).
     */
    @Value("${jpeg.compression.quality:0.8}")
    private float initialQuality;

    private static final int MAX_ITERATIONS = 10;
    private static final double TOLERANCE = 0.05; // 5%

    /**
     * Compresses the provided image bytes and saves the result to the output directory.
     *
     * @param imageBytes      the original image bytes
     * @param outputDirectory the directory where the compressed image will be saved
     * @return a CompressionResult containing the compressed size and file link
     * @throws IOException if an I/O error occurs or the image cannot be processed
     */
    public CompressionResult compressAndSave(byte[] imageBytes, Path outputDirectory) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Received an empty byte array.");
        }
        log.debug("Original image size: {} bytes.", imageBytes.length);

        BufferedImage bufferedImage = decodeImage(imageBytes);
        if (bufferedImage == null) {
            throw new IOException("Failed to decode image.");
        }

        // Ensure image is in RGB format
        if (bufferedImage.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgbImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgbImage.createGraphics();
            graphics.drawImage(bufferedImage, 0, 0, null);
            graphics.dispose();
            bufferedImage = rgbImage;
        }

        // Target file size: half of the original
        long targetSize = imageBytes.length / 2;
        log.debug("Target file size: {} bytes", targetSize);

        // Find the optimal quality to meet the target size
        float optimalQuality = findOptimalQuality(bufferedImage, targetSize);

        // Generate a unique filename using UUID
        String outputFileName = UUID.randomUUID().toString() + ".jpg";
        Path outputPath = outputDirectory.resolve(outputFileName);

        try (OutputStream os = Files.newOutputStream(outputPath)) {
            compressJpeg(bufferedImage, os, optimalQuality);
        }
        long compressedSize = Files.size(outputPath);
        log.info("Final file size: {} bytes at quality: {}", compressedSize, optimalQuality);
        return new CompressionResult(compressedSize, outputPath.toString());
    }

    /**
     * Uses a binary search to determine the optimal JPEG quality setting that produces a file close to the target size.
     *
     * @param image      the BufferedImage to compress
     * @param targetSize the desired file size in bytes
     * @return the optimal quality factor (between 0.0 and 1.0)
     * @throws IOException if an error occurs during compression
     */
    private float findOptimalQuality(BufferedImage image, long targetSize) throws IOException {
        float low = 0.0f;
        float high = 1.0f;
        float bestQuality = initialQuality;

        byte[] maxQualityBytes = compressJpegToByteArray(image, 1.0f);
        if (maxQualityBytes.length <= targetSize) {
            return 1.0f;
        }

        byte[] minQualityBytes = compressJpegToByteArray(image, 0.0f);
        if (minQualityBytes.length > targetSize) {
            return 0.0f;
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            float mid = (low + high) / 2.0f;
            byte[] compressedBytes = compressJpegToByteArray(image, mid);
            int size = compressedBytes.length;
            log.debug("Iteration {}: quality = {}, size = {}", i, mid, size);

            if (Math.abs(size - targetSize) < targetSize * TOLERANCE) {
                bestQuality = mid;
                break;
            }
            if (size > targetSize) {
                high = mid;
            } else {
                low = mid;
            }
            bestQuality = mid;
        }
        return bestQuality;
    }

    /**
     * Compresses the given image to a JPEG byte array with the specified quality.
     *
     * @param image   the image to compress
     * @param quality the JPEG quality (0.0 to 1.0)
     * @return the compressed image as a byte array
     * @throws IOException if an error occurs during compression
     */
    private byte[] compressJpegToByteArray(BufferedImage image, float quality) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            compressJpeg(image, baos, quality);
            return baos.toByteArray();
        }
    }

    /**
     * Decodes the image from the provided byte array. Supports JPEG and attempts WebP if needed.
     *
     * @param imageBytes the raw image bytes
     * @return the decoded BufferedImage, or null if decoding fails
     * @throws IOException if an error occurs during reading
     */
    private BufferedImage decodeImage(byte[] imageBytes) throws IOException {
        BufferedImage img;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            img = ImageIO.read(bais);
        }
        if (img != null) {
            return img;
        }
        if (isWebP(imageBytes)) {
            log.info("WebP format detected. Attempting to decode WebP image.");
            return decodeWebP(imageBytes);
        }
        return null;
    }

    /**
     * Checks if the provided bytes represent a WebP image.
     *
     * @param imageBytes the image bytes
     * @return true if the image is WebP; false otherwise
     */
    private boolean isWebP(byte[] imageBytes) {
        if (imageBytes.length < 12) return false;
        String header = new String(imageBytes, 0, 12, StandardCharsets.US_ASCII);
        return header.startsWith("RIFF") && header.substring(8, 12).equals("WEBP");
    }

    /**
     * Attempts to decode a WebP image.
     *
     * @param imageBytes the image bytes
     * @return the decoded BufferedImage, or null if decoding fails
     * @throws IOException if an error occurs during reading
     */
    private BufferedImage decodeWebP(byte[] imageBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.error("Failed to decode WebP image. Ensure the appropriate plugin is installed.");
            }
            return img;
        }
    }

    /**
     * Compresses the BufferedImage as a JPEG and writes it to the provided OutputStream.
     *
     * @param image   the image to compress
     * @param os      the output stream to write the compressed image
     * @param quality the JPEG quality (0.0 to 1.0)
     * @throws IOException if an error occurs during writing
     */
    private void compressJpeg(BufferedImage image, OutputStream os, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No writers found for jpg");
        }
        ImageWriter jpgWriter = writers.next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(quality);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            jpgWriter.setOutput(ios);
            jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
        } finally {
            jpgWriter.dispose();
        }
    }
}
