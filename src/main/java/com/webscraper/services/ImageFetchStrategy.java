package com.webscraper.services;


public interface ImageFetchStrategy {

    boolean supports(String imageUrl);

    byte[] fetchImage(String imageUrl, ImageProcessingService context);
}