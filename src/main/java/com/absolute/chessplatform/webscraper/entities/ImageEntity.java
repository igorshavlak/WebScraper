package com.absolute.chessplatform.webscraper.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(unique=true)
    private String originalUrl;
    private String path;
    private String originalSize;
    private String sizeAfterCompression;
}
