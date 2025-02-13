package com.webscraper.services;

public interface ImageProcessingService {

    void processImage(String imagePath, String domain);

    String prepareImageUrl(String imageUrl);
}