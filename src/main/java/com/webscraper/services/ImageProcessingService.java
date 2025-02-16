package com.webscraper.services;

/**
 * Service interface for processing images, including compression and URL preparation.
 */
public interface ImageProcessingService {

    /**
     * Processes the image at the given path for the specified domain.
     *
     * @param imagePath the URL or file path of the image
     * @param domain    the domain associated with the image
     */
    void processImage(String imagePath, String domain);

    /**
     * Prepares the image URL by decoding and removing unnecessary parameters.
     *
     * @param imageUrl the original image URL
     * @return the prepared image URL
     */
    String prepareImageUrl(String imageUrl);
}
