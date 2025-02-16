package com.webscraper.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing an image and its compression metadata.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "original_url", unique = true)
    private String originalUrl;

    private String path;

    @Column(name = "original_size")
    private long originalSize;

    @Column(name = "size_after_compression")
    private long sizeAfterCompression;
}
