package com.webscraper.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;


/**
 * POJO representing the request body for a scraping task.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScraperBody {

    private String title;
    private String url;
    private int recursionDepth;
    private Long requestDelay;
    private List<ProxyInfo> proxies;
}
