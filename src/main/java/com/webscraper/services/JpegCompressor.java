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

@Service
@Slf4j
public class JpegCompressor {

    // Початкове значення якості (можна задавати через application.properties)
    @Value("${jpeg.compression.quality:0.8}")
    private float initialQuality;

    // Кількість ітерацій пошуку та допустима похибка (відносна різниця)
    private static final int MAX_ITERATIONS = 10;
    private static final double TOLERANCE = 0.05; // 5%

    public CompressionResult compressAndSave(byte[] imageBytes, Path outputDirectory) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Отримано порожній масив байтів.");
        }
        log.debug("Отримано зображення розміром: {} байтів.", imageBytes.length);

        BufferedImage bufferedImage = decodeImage(imageBytes);
        if (bufferedImage == null) {
            throw new IOException("Не вдалося декодувати зображення.");
        }

        // Переконуємося, що зображення має формат RGB
        if (bufferedImage.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgbImage = new BufferedImage(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgbImage.createGraphics();
            graphics.drawImage(bufferedImage, 0, 0, null);
            graphics.dispose();
            bufferedImage = rgbImage;
        }

        // Цільовий розмір файлу – вдвічі менше за розмір вхідного масиву байтів
        long targetSize = imageBytes.length / 2;
        log.debug("Цільовий розмір файлу: {} байтів", targetSize);

        // Знаходимо оптимальне значення якості, яке дасть бажаний розмір файлу
        float optimalQuality = findOptimalQuality(bufferedImage, targetSize);

        // Генеруємо унікальне ім'я файлу за допомогою UUID
        String outputFileName = UUID.randomUUID().toString() + ".jpg";
        Path outputPath = outputDirectory.resolve(outputFileName);

        try (OutputStream os = Files.newOutputStream(outputPath)) {
            compressJpeg(bufferedImage, os, optimalQuality);
        }
        long compressedSize = Files.size(outputPath);
        log.info("Остаточний розмір файлу: {} байтів при якості: {}", compressedSize, optimalQuality);
        return new CompressionResult(compressedSize, outputPath.toString());
    }

    /**
     * Метод виконує бінарний пошук по значенню якості JPEG,
     * щоб отримати результат із розміром файлу приблизно рівним targetSize.
     */
    private float findOptimalQuality(BufferedImage image, long targetSize) throws IOException {
        float low = 0.0f;
        float high = 1.0f;
        float bestQuality = initialQuality; // Початкове припущення

        // Перевірка: якщо при максимальній якості отриманий розмір уже менший за цільовий,
        // то повертаємо максимальну якість.
        byte[] maxQualityBytes = compressJpegToByteArray(image, 1.0f);
        if (maxQualityBytes.length <= targetSize) {
            return 1.0f;
        }
        // Аналогічно: якщо при мінімальній якості розмір все одно перевищує цільовий,
        // повертаємо мінімальну якість.
        byte[] minQualityBytes = compressJpegToByteArray(image, 0.0f);
        if (minQualityBytes.length > targetSize) {
            return 0.0f;
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            float mid = (low + high) / 2.0f;
            byte[] compressedBytes = compressJpegToByteArray(image, mid);
            int size = compressedBytes.length;
            log.debug("Ітерація {}: якість = {}, розмір = {}", i, mid, size);

            // Якщо отриманий розмір близький до цільового (в межах TOLERANCE),
            // вважаємо, що знайшли оптимальну якість.
            if (Math.abs(size - targetSize) < targetSize * TOLERANCE) {
                bestQuality = mid;
                break;
            }
            if (size > targetSize) {
                // Якщо розмір файлу перевищує ціль, зменшуємо якість.
                high = mid;
            } else {
                // Якщо розмір файлу менший за ціль, збільшуємо якість.
                low = mid;
            }
            bestQuality = mid;
        }
        return bestQuality;
    }

    /**
     * Стискає зображення із заданою якістю у масив байтів.
     */
    private byte[] compressJpegToByteArray(BufferedImage image, float quality) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            compressJpeg(image, baos, quality);
            return baos.toByteArray();
        }
    }

    private BufferedImage decodeImage(byte[] imageBytes) throws IOException {
        BufferedImage img;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            img = ImageIO.read(bais);
        }
        if (img != null) {
            return img;
        }
        if (isWebP(imageBytes)) {
            log.info("Виявлено формат WebP. Спроба декодувати зображення WebP.");
            return decodeWebP(imageBytes);
        }
        return null;
    }

    private boolean isWebP(byte[] imageBytes) {
        if (imageBytes.length < 12) return false;
        String header = new String(imageBytes, 0, 12, StandardCharsets.US_ASCII);
        return header.startsWith("RIFF") && header.substring(8, 12).equals("WEBP");
    }

    private BufferedImage decodeWebP(byte[] imageBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.error("Не вдалося декодувати WebP зображення. Переконайтеся, що підключено відповідний плагін.");
            }
            return img;
        }
    }

    /**
     * Стискає BufferedImage у JPEG і записує результат в OutputStream.
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
