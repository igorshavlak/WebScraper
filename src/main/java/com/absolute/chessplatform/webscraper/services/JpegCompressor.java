package com.absolute.chessplatform.webscraper.services;

import com.absolute.chessplatform.webscraper.entities.CompressionResult;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

@Service
public class JpegCompressor {

    public CompressionResult compressAndSave(byte[] imageBytes, Path outputDirectory) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            BufferedImage bufferedImage = ImageIO.read(bais);
            if (bufferedImage == null) {
                throw new IOException("Не вдалося декодувати зображення.");
            }

            if (bufferedImage.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage rgbImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = rgbImage.createGraphics();
                graphics.drawImage(bufferedImage, 0, 0, null);
                graphics.dispose();
                bufferedImage = rgbImage;
            }

            float quality = 0.8f;
            String outputFileName = System.currentTimeMillis() + ".jpg";
            Path outputPath = outputDirectory.resolve(outputFileName);
            try (OutputStream os = Files.newOutputStream(outputPath)) {
                compressJpeg(bufferedImage, os, quality);
            }
            long compressedSize = Files.size(outputPath);
            return new CompressionResult(compressedSize, outputPath.toString());
        }

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
