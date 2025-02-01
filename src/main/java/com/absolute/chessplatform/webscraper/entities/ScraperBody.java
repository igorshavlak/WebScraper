package com.absolute.chessplatform.webscraper.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScraperBody {
    private String title;
    private String url;
    private int recursionDepth;
}