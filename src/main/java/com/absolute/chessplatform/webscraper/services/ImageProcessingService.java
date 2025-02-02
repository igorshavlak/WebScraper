package com.absolute.chessplatform.webscraper.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.Iterator;
import java.util.Random;

@Slf4j
@Service
public class ImageProcessingService {

    private static final String OUTPUT_DIRECTORY = "C:\\Users\\igors\\IdeaProjects\\WebScraper\\src\\main\\resources\\compressed-images";

    public ImageProcessingService() {
        File dir = new File(OUTPUT_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void processImage(String imagePath) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        byte[] imageBytes = restTemplate.getForObject(imagePath, byte[].class);
        if(imageBytes == null || imageBytes.length < 200 * 1024) {
            return;
        }
        compressImageAndSave(imageBytes);
    }

    public void compressImageAndSave(byte[] imageBytes) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (bufferedImage == null) {
            log.warn("Failed to decode image from bytes");
            return;
        }
        float quality = 0.8f;
        String outputFilePath = OUTPUT_DIRECTORY + File.separator + System.currentTimeMillis() + ".jpg";
        File outputFile = new File(outputFilePath);
        try (FileOutputStream finalOutputStream = new FileOutputStream(outputFile)) {
            compressJpeg(bufferedImage, finalOutputStream, quality);
        }
        log.info("Saved compressed image to {}", outputFilePath);
    }

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
