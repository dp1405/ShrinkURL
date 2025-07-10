package org.stir.shrinkurl.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;
import org.stir.shrinkurl.utils.UrlShortenerUtil;

// @Component
@Slf4j
public class UrlShortenerTestRunner implements CommandLineRunner {

    @Autowired
    private UrlShortenerUtil urlShortenerUtil;

    @Override
    public void run(String... args) throws Exception {
        // Test URL shortening algorithm
        testUrlShortening();
    }

    private void testUrlShortening() {
        log.info("Testing URL shortening algorithm...");
        
        String[] testUrls = {
            "https://www.google.com",
            "https://www.github.com",
            "https://www.stackoverflow.com",
            "https://www.example.com/very/long/path/with/many/segments"
        };
        
        for (String url : testUrls) {
            String shortCode = urlShortenerUtil.generateShortCode(url);
            log.info("URL: {} -> Short Code: {}", url, shortCode);
            
            // Test collision handling
            String shortCodeWithSalt = urlShortenerUtil.generateShortCodeWithSalt(url, 1);
            log.info("URL: {} -> Short Code with Salt: {}", url, shortCodeWithSalt);
        }
        
        // Test custom code validation
        String[] customCodes = {"abc123", "test", "a", "verylongcustomcode123456789", "invalid@code"};
        for (String code : customCodes) {
            boolean isValid = urlShortenerUtil.isValidCustomCode(code);
            log.info("Custom Code: {} -> Valid: {}", code, isValid);
        }
    }
}
