package com.webscraper.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

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
