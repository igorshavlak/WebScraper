package com.webscraper.entities;

/**
 * Record representing the result of an image compression operation.
 *
 * @param compressedSize the size of the compressed image in bytes
 * @param fileLink       the file path or link to the compressed image
 */
public record CompressionResult(long compressedSize, String fileLink) {
}