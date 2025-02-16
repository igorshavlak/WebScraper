package com.webscraper.services.strategy;

import com.webscraper.services.ImageProcessingService;

/**
 * Strategy interface for fetching image data.
 */
public interface ImageFetchStrategy {

    /**
     * Determines if this strategy supports fetching the image from the given URL.
     *
     * @param imageUrl the URL of the image
     * @return true if the strategy supports the given URL; false otherwise
     */
    boolean supports(String imageUrl);

    /**
     * Fetches the image data as a byte array.
     *
     * @param imageUrl the URL of the image
     * @param context  the {@link ImageProcessingService} context used during processing
     * @return a byte array containing the image data, or null if fetching fails
     */
    byte[] fetchImage(String imageUrl, ImageProcessingService context);
}
